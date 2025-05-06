package com.fintrack.service.subscription;

import com.fintrack.constants.subscription.SubscriptionPlanType;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserSubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionService.class);
    
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentService paymentService;
    private final SubscriptionPlanService subscriptionPlanService;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public UserSubscriptionService(
            UserSubscriptionRepository userSubscriptionRepository,
            PaymentService paymentService,
            SubscriptionPlanService subscriptionPlanService) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.paymentService = paymentService;
        this.subscriptionPlanService = subscriptionPlanService;
    }

    public Optional<UserSubscription> getSubscriptionByAccountId(UUID accountId) {
        return userSubscriptionRepository.findByAccountId(accountId);
    }

    /**
     * Creates a free subscription for a new user
     * @param accountId The user's account ID
     * @param planName The name of the free plan (typically "Free")
     * @return The created UserSubscription
     */
    public UserSubscription createFreeSubscription(UUID accountId, String planName) {
        logger.info("Creating free subscription for new user: {}", accountId);
        return handleFreePlanSubscription(accountId, planName);
    }

    @Transactional
    public UserSubscription updateSubscription(UUID accountId, String planName, String paymentMethodId) throws StripeException {
        logger.info("Updating subscription for account: {} with plan: {}", accountId, planName);
        
        SubscriptionPlanType planType = SubscriptionPlanType.fromPlanName(planName);
        if (planType == null) {
            throw new IllegalArgumentException("Invalid plan name: " + planName);
        }
        
        // If it's a free plan, handle it differently
        if (planType == SubscriptionPlanType.FREE) {
            return handleFreePlanSubscription(accountId, planType.getPlanName());
        }
        
        Stripe.apiKey = stripeSecretKey;

        // Get plan ID from plan name
        String planId = subscriptionPlanService.getPlanIdByName(planType.getPlanName());
        String stripePriceId = subscriptionPlanService.getStripePriceIdByName(planType.getPlanName());
        
        logger.info("Found plan ID: {} and Stripe price ID: {} for plan name: {}", planId, stripePriceId, planType.getPlanName());

        // Get current subscription if exists
        Optional<UserSubscription> currentSubscription = userSubscriptionRepository.findByAccountId(accountId);

        if (currentSubscription.isPresent()) {
            // Handle downgrade to free
            if (planType == SubscriptionPlanType.FREE) {
                return handleDowngradeToFreePlan(currentSubscription.get(), planId);
            }
            
            // Update existing subscription
            return updateExistingSubscription(currentSubscription.get(), planId, stripePriceId, paymentMethodId);
        } else {
            // Create new subscription
            return createNewSubscription(accountId, planId, stripePriceId, paymentMethodId);
        }
    }
    
    private UserSubscription handleFreePlanSubscription(UUID accountId, String planName) {
        logger.info("Handling free plan subscription for account: {}", accountId);
        
        String planId = subscriptionPlanService.getPlanIdByName(planName);
        
        // Check if user already has a subscription
        Optional<UserSubscription> existingSubscription = userSubscriptionRepository.findByAccountId(accountId);
        
        if (existingSubscription.isPresent()) {
            return handleDowngradeToFreePlan(existingSubscription.get(), planId);
        } else {
            // Create a new free subscription
            UserSubscription subscription = new UserSubscription();
            subscription.setAccountId(accountId);
            subscription.setPlanId(planId);
            subscription.setStripeSubscriptionId("free_" + accountId); // Just a placeholder
            subscription.setStripeCustomerId(accountId.toString());
            subscription.setActive(true);
            subscription.setStatus("active");
            subscription.setSubscriptionStartDate(LocalDateTime.now());
            subscription.setCreatedAt(LocalDateTime.now());
            
            // Free plans don't expire
            subscription.setNextBillingDate(LocalDateTime.now().plusYears(100));
            
            return userSubscriptionRepository.save(subscription);
        }
    }
    
    private UserSubscription handleDowngradeToFreePlan(UserSubscription existingSubscription, String freePlanId) {
        logger.info("Downgrading subscription to free plan for account: {}", existingSubscription.getAccountId());
        
        // If there's an active Stripe subscription, we should cancel it
        try {
            if (existingSubscription.getStripeSubscriptionId() != null && 
                !existingSubscription.getStripeSubscriptionId().startsWith("free_")) {
                
                Stripe.apiKey = stripeSecretKey;
                Subscription stripeSubscription = Subscription.retrieve(existingSubscription.getStripeSubscriptionId());
                stripeSubscription.cancel();
                
                logger.info("Canceled Stripe subscription: {}", existingSubscription.getStripeSubscriptionId());
            }
        } catch (Exception e) {
            logger.error("Error canceling Stripe subscription: {}", e.getMessage());
            // Continue with the downgrade even if cancellation fails
        }
        
        // Update the subscription to free plan
        existingSubscription.setPlanId(freePlanId);
        existingSubscription.setStripeSubscriptionId("free_" + existingSubscription.getAccountId());
        existingSubscription.setActive(true);
        existingSubscription.setStatus("active");
        existingSubscription.setNextBillingDate(LocalDateTime.now().plusYears(100));
        existingSubscription.setCancelAtPeriodEnd(false);
        
        return userSubscriptionRepository.save(existingSubscription);
    }

    private UserSubscription updateExistingSubscription(UserSubscription currentSubscription, String planId, String stripePriceId, String paymentMethodId) throws StripeException {
        logger.info("Updating existing subscription: {} with plan ID: {} and Stripe price ID: {}", 
                currentSubscription.getStripeSubscriptionId(), planId, stripePriceId);
        
        // Skip Stripe call if it's a free subscription being upgraded
        if (currentSubscription.getStripeSubscriptionId().startsWith("free_")) {
            logger.info("Upgrading from free plan to paid plan: {}", planId);
            // Create a new subscription in Stripe instead of updating
            return createNewSubscription(currentSubscription.getAccountId(), planId, stripePriceId, paymentMethodId);
        }
        
        // Update payment method if provided
        if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
            // Verify payment method belongs to the account
            paymentService.getPaymentMethods(currentSubscription.getAccountId()).stream()
                    .filter(pm -> pm.getStripePaymentMethodId().equals(paymentMethodId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Payment method not found for this account"));

            // Update default payment method in Stripe
            Customer customer = Customer.retrieve(currentSubscription.getStripeCustomerId());
            customer.update(Map.of("invoice_settings", Map.of("default_payment_method", paymentMethodId)));
        }

        // First retrieve the subscription to get the subscription item ID
        Subscription stripeSubscription = Subscription.retrieve(currentSubscription.getStripeSubscriptionId());
        
        // Check if subscription has items
        if (stripeSubscription.getItems() == null || stripeSubscription.getItems().getData() == null || 
            stripeSubscription.getItems().getData().isEmpty()) {
            throw new RuntimeException("Subscription has no items to update");
        }
        
        // Get the first subscription item's ID
        String subscriptionItemId = stripeSubscription.getItems().getData().get(0).getId();
        logger.info("Found subscription item ID: {} for subscription: {}", 
                subscriptionItemId, currentSubscription.getStripeSubscriptionId());

        // Update subscription in Stripe
        SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                .addItem(SubscriptionUpdateParams.Item.builder()
                        .setId(subscriptionItemId)
                        .setPrice(stripePriceId)
                        .build())
                .build();

        stripeSubscription = stripeSubscription.update(params);

        // Calculate next billing date from Stripe response
        LocalDateTime nextBillingDate;
        // Get the current period end from the subscription
        if (stripeSubscription.getItems() != null && stripeSubscription.getItems().getData() != null && 
            !stripeSubscription.getItems().getData().isEmpty()) {
            // Get the subscription's current period end
            Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
            if (periodEnd != null) {
                nextBillingDate = LocalDateTime.ofEpochSecond(periodEnd, 0, ZoneOffset.UTC);
            } else {
                nextBillingDate = LocalDateTime.now().plusDays(30); // Fallback to 30 days
            }
        } else {
            nextBillingDate = LocalDateTime.now().plusDays(30); // Fallback to 30 days
        }

        // Update subscription in our database
        currentSubscription.setPlanId(planId);
        currentSubscription.setStatus(stripeSubscription.getStatus());
        currentSubscription.setActive("active".equals(stripeSubscription.getStatus()));
        currentSubscription.setNextBillingDate(nextBillingDate);
        currentSubscription.setLastPaymentDate(LocalDateTime.now());
        currentSubscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd());

        return userSubscriptionRepository.save(currentSubscription);
    }

    private UserSubscription createNewSubscription(UUID accountId, String planId, String stripePriceId, String paymentMethodId) throws StripeException {
        logger.info("Creating new subscription for account: {} with plan ID: {} and Stripe price ID: {}", 
                accountId, planId, stripePriceId);
        
        // Verify payment method if provided
        if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
            paymentService.getPaymentMethods(accountId).stream()
                    .filter(pm -> pm.getStripePaymentMethodId().equals(paymentMethodId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Payment method not found for this account"));
        }

        // Ensure customer exists in Stripe
        String customerId = ensureStripeCustomerExists(accountId);
        
        // Create subscription in Stripe
        SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(stripePriceId)
                        .build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .setPaymentSettings(SubscriptionCreateParams.PaymentSettings.builder()
                        .setPaymentMethodTypes(List.of(SubscriptionCreateParams.PaymentSettings.PaymentMethodType.CARD))
                        .setSaveDefaultPaymentMethod(SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                        .build());

        if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
            paramsBuilder.setDefaultPaymentMethod(paymentMethodId);
        }

        Subscription stripeSubscription = Subscription.create(paramsBuilder.build());
        
        // Calculate next billing date from Stripe response
        LocalDateTime nextBillingDate;
        // Get the current period end from the subscription
        if (stripeSubscription.getItems() != null && stripeSubscription.getItems().getData() != null && 
            !stripeSubscription.getItems().getData().isEmpty()) {
            // Get the subscription's current period end
            Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
            if (periodEnd != null) {
                nextBillingDate = LocalDateTime.ofEpochSecond(periodEnd, 0, ZoneOffset.UTC);
            } else {
                nextBillingDate = LocalDateTime.now().plusDays(30); // Fallback to 30 days
            }
        } else {
            nextBillingDate = LocalDateTime.now().plusDays(30); // Fallback to 30 days
        }

        // Save subscription in our database
        UserSubscription subscription = new UserSubscription();
        subscription.setAccountId(accountId);
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStripeCustomerId(customerId);
        subscription.setPlanId(planId);
        subscription.setStatus(stripeSubscription.getStatus());
        subscription.setActive("active".equals(stripeSubscription.getStatus()));
        subscription.setSubscriptionStartDate(LocalDateTime.now());
        subscription.setNextBillingDate(nextBillingDate);
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setCancelAtPeriodEnd(stripeSubscription.getCancelAtPeriodEnd());

        // Delete any existing subscription for this account
        userSubscriptionRepository.findByAccountId(accountId).ifPresent(existing -> {
            logger.info("Deleting existing subscription record for account: {}", accountId);
            userSubscriptionRepository.delete(existing);
        });

        return userSubscriptionRepository.save(subscription);
    }
    
    private String ensureStripeCustomerExists(UUID accountId) throws StripeException {
        String customerId = accountId.toString();
        
        try {
            // Try to retrieve the customer
            Customer.retrieve(customerId);
            logger.info("Found existing Stripe customer with ID: {}", customerId);
        } catch (StripeException e) {
            if (e.getCode().equals("resource_missing") && e.getMessage().contains("No such customer")) {
                // Customer doesn't exist, create a new one
                logger.info("Customer not found in Stripe, creating new customer with ID: {}", customerId);
                Map<String, Object> params = new HashMap<>();
                params.put("id", customerId);
                params.put("description", "Customer for account ID: " + accountId);
                
                Customer customer = Customer.create(params);
                customerId = customer.getId();
                logger.info("Created new Stripe customer with ID: {}", customerId);
            } else {
                // Some other Stripe error occurred
                logger.error("Error retrieving Stripe customer: {}", e.getMessage());
                throw e;
            }
        }
        
        return customerId;
    }
}