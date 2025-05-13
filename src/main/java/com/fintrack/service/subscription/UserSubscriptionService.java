package com.fintrack.service.subscription;

import com.fintrack.constants.subscription.SubscriptionPlanType;
import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.Arrays;

@Service
public class UserSubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionService.class);
    
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentService paymentService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final com.fintrack.repository.payment.PaymentIntentRepository paymentIntentRepository;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public UserSubscriptionService(
            UserSubscriptionRepository userSubscriptionRepository,
            PaymentService paymentService,
            SubscriptionPlanService subscriptionPlanService,
            com.fintrack.repository.payment.PaymentIntentRepository paymentIntentRepository) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.paymentService = paymentService;
        this.subscriptionPlanService = subscriptionPlanService;
        this.paymentIntentRepository = paymentIntentRepository;
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
    public UserSubscription updateSubscription(UUID accountId, String planId, String paymentMethodId) throws StripeException {
        logger.info("Updating subscription for account: {} with plan ID: {}", accountId, planId);
        
        // Get plan details
        SubscriptionPlan plan = subscriptionPlanService.getPlanById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));
        
        String stripePriceId = plan.getStripePriceId();
        
        logger.info("Found plan ID: {} and Stripe price ID: {} for plan", planId, stripePriceId);

        // Get current subscription if exists
        Optional<UserSubscription> currentSubscription = userSubscriptionRepository.findByAccountId(accountId);

        if (currentSubscription.isPresent()) {
            // Handle downgrade to free
            SubscriptionPlanType planType = SubscriptionPlanType.fromPlanId(planId);
            if (planType == SubscriptionPlanType.FREE) {
                return handleDowngradeToFreePlan(currentSubscription.get(), planId);
            }
            
            // Update existing subscription
            return updateExistingSubscription(currentSubscription.get(), planId, stripePriceId, paymentMethodId);
        } else {
            // If it's a free plan, handle it differently
            SubscriptionPlanType planType = SubscriptionPlanType.fromPlanId(planId);
            if (planType == SubscriptionPlanType.FREE) {
                return handleFreePlanSubscription(accountId, planType.getPlanName());
            }
            
            Stripe.apiKey = stripeSecretKey;
            
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

    @Transactional
    public SubscriptionUpdateResponse upgradeSubscriptionWithPayment(UUID accountId, String planId, 
            String paymentMethodId, String returnUrl) throws StripeException {
        // STEP 1: User initiates payment (This method is called from UserSubscriptionController)
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 1: Payment Initiation");
        logger.trace("║ Account: {}", accountId);
        logger.trace("║ Plan ID: {}", planId);
        logger.trace("║ Payment Method: {}", paymentMethodId);
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
        // Get plan details
        SubscriptionPlan plan = subscriptionPlanService.getPlanById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));
        
        String stripePriceId = plan.getStripePriceId();
        
        logger.trace("║ Plan Details Retrieved:");
        logger.trace("║ - Plan ID: {}", planId);
        logger.trace("║ - Stripe Price ID: {}", stripePriceId);
        logger.trace("║ - Amount: {}", plan.getAmount());
        logger.trace("║ - Currency: {}", plan.getCurrency());

        // STEP 2: Backend prepares for payment processing
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 2: Payment Processing Preparation");
        logger.trace("╚══════════════════════════════════════════════════════════════");

        // Configure Stripe API
        Stripe.apiKey = stripeSecretKey;
        
        // Get current subscription
        Optional<UserSubscription> currentSubscription = userSubscriptionRepository.findByAccountId(accountId);
        logger.trace("║ Current Subscription Status:");
        logger.trace("║ - Exists: {}", currentSubscription.isPresent());
        if (currentSubscription.isPresent()) {
            logger.trace("║ - Current Plan: {}", currentSubscription.get().getPlanId());
            logger.trace("║ - Status: {}", currentSubscription.get().getStatus());
        }

        // Handle free plan upgrades differently
        if (currentSubscription.isPresent() && currentSubscription.get().getStripeSubscriptionId().startsWith("free_")) {
            return handleFreeToPaidUpgrade(currentSubscription.get(), planId, stripePriceId, 
                    paymentMethodId, plan.getAmount(), plan.getCurrency(), returnUrl);
        }

        // STEP 3: Create or Update Stripe Subscription
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 3: Create/Update Stripe Resources");
        logger.trace("╚══════════════════════════════════════════════════════════════");

        if (currentSubscription.isPresent()) {
            return updateExistingSubscriptionWithPayment(currentSubscription.get(), planId, 
                    stripePriceId, paymentMethodId, plan.getAmount(), plan.getCurrency(), returnUrl);
        } else {
            return createNewSubscriptionWithPayment(accountId, planId, stripePriceId, 
                    paymentMethodId, plan.getAmount(), plan.getCurrency(), returnUrl);
        }
    }

    private SubscriptionUpdateResponse handleFreeToPaidUpgrade(UserSubscription currentSubscription,
            String planId, String stripePriceId, String paymentMethodId, BigDecimal amount, 
            String currency, String returnUrl) throws StripeException {
        
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 3A: Handling Free to Paid Upgrade");
        logger.trace("║ - Current Plan: {}", currentSubscription.getPlanId());
        logger.trace("║ - New Plan: {}", planId);
        logger.trace("╚══════════════════════════════════════════════════════════════");

        // Verify and update payment method
        String customerId = currentSubscription.getStripeCustomerId();
        logger.trace("║ Verifying Payment Method");
        logger.trace("║ - Customer ID: {}", customerId);
        logger.trace("║ - Payment Method: {}", paymentMethodId);

        try {
            Customer customer = Customer.retrieve(customerId);
            customer.update(Map.of("invoice_settings", Map.of("default_payment_method", paymentMethodId)));
            logger.trace("✓ Payment method updated successfully");
        } catch (Exception e) {
            logger.error("❌ Error updating payment method: {}", e.getMessage());
            throw new RuntimeException("Failed to update payment method: " + e.getMessage());
        }

        // Create Stripe subscription
        logger.trace("║ Creating Stripe Subscription");
        SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(stripePriceId)
                        .build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .setPaymentSettings(SubscriptionCreateParams.PaymentSettings.builder()
                        .setPaymentMethodTypes(List.of(SubscriptionCreateParams.PaymentSettings.PaymentMethodType.CARD))
                        .setSaveDefaultPaymentMethod(SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                        .build())
                .setDefaultPaymentMethod(paymentMethodId)
                .build();

        Subscription stripeSubscription = Subscription.create(params);
        logger.trace("✓ Stripe subscription created");
        logger.trace("║ - Subscription ID: {}", stripeSubscription.getId());
        logger.trace("║ - Status: {}", stripeSubscription.getStatus());

        // STEP 4: Create Payment Intent
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 4: Create Payment Intent");
        logger.trace("╚══════════════════════════════════════════════════════════════");

        Map<String, Object> paymentIntentParams = new HashMap<>();
        int amountInCents = amount.multiply(BigDecimal.valueOf(100)).intValue();
        paymentIntentParams.put("amount", amountInCents);
        paymentIntentParams.put("currency", currency.toLowerCase());
        paymentIntentParams.put("customer", customerId);
        paymentIntentParams.put("payment_method", paymentMethodId);
        paymentIntentParams.put("payment_method_types", Arrays.asList("card"));
        paymentIntentParams.put("setup_future_usage", "off_session");
        paymentIntentParams.put("metadata", Map.of(
            "subscription_id", stripeSubscription.getId(),
            "account_id", currentSubscription.getAccountId().toString()
        ));

        logger.trace("║ Payment Intent Parameters:");
        logger.trace("║ - Amount: {} cents", amountInCents);
        logger.trace("║ - Currency: {}", currency);
        logger.trace("║ - Setup Future Usage: {}", paymentIntentParams.get("setup_future_usage"));

        PaymentIntent paymentIntent = PaymentIntent.create(paymentIntentParams);
        logger.trace("✓ Payment Intent created");
        logger.trace("║ - ID: {}", paymentIntent.getId());
        logger.trace("║ - Status: {}", paymentIntent.getStatus());

        // STEP 5: Update Database
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 5: Update Local Database");
        logger.trace("╚══════════════════════════════════════════════════════════════");

        currentSubscription.setStripeSubscriptionId(stripeSubscription.getId());
        currentSubscription.setPlanId(planId);
        currentSubscription.setStatus("pending_payment");
        currentSubscription.setActive(false);
        currentSubscription = userSubscriptionRepository.save(currentSubscription);
        logger.trace("✓ Database updated");
        logger.trace("║ - New Status: {}", currentSubscription.getStatus());
        logger.trace("║ - New Plan ID: {}", currentSubscription.getPlanId());

        // STEP 6: Return Response
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 6: Return Client Secret");
        logger.trace("║ Payment flow ready for frontend confirmation");
        logger.trace("╚══════════════════════════════════════════════════════════════");

        return SubscriptionUpdateResponse.fromUserSubscription(currentSubscription, 
                paymentIntent.getClientSecret(), true, amount, currency);
    }

    private SubscriptionUpdateResponse createNewSubscriptionWithPayment(UUID accountId, String planId,
            String stripePriceId, String paymentMethodId, BigDecimal amount, String currency,
            String returnUrl) throws StripeException {
        
        logger.trace("Creating new subscription with payment - Account: {}, Plan: {}, Amount: {}", 
                accountId, planId, amount);
        
        // Ensure customer exists in Stripe
        String customerId = ensureStripeCustomerExists(accountId);
        logger.trace("Customer ID confirmed in Stripe: {}", customerId);

        // Create subscription in Stripe first
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
        logger.trace("Stripe subscription created - ID: {}, Status: {}", 
                stripeSubscription.getId(), stripeSubscription.getStatus());
        
        // Create a payment intent for the new subscription
        Map<String, Object> paymentIntentParams = new HashMap<>();
        int amountInCents = amount.multiply(BigDecimal.valueOf(100)).intValue();
        paymentIntentParams.put("amount", amountInCents);
        paymentIntentParams.put("currency", currency.toLowerCase());
        paymentIntentParams.put("customer", customerId);
        paymentIntentParams.put("payment_method", paymentMethodId);
        paymentIntentParams.put("payment_method_types", Arrays.asList("card"));
        paymentIntentParams.put("confirm", true);
        paymentIntentParams.put("setup_future_usage", "off_session");
        paymentIntentParams.put("return_url", returnUrl);
        paymentIntentParams.put("capture_method", "automatic");
        paymentIntentParams.put("metadata", Map.of(
            "account_id", accountId.toString(),
            "plan_id", planId,
            "subscription_id", stripeSubscription.getId()
        ));

        logger.trace("Payment intent parameters prepared for new subscription - Amount (cents): {}", amountInCents);

        PaymentIntent paymentIntent = PaymentIntent.create(paymentIntentParams);
        logger.trace("Payment intent created for new subscription - ID: {}, Status: {}", 
                paymentIntent.getId(), paymentIntent.getStatus());

        // Save payment intent to our database
        com.fintrack.model.payment.PaymentIntent dbPaymentIntent = new com.fintrack.model.payment.PaymentIntent();
        dbPaymentIntent.setAccountId(accountId);
        dbPaymentIntent.setStripePaymentIntentId(paymentIntent.getId());
        dbPaymentIntent.setAmount(amount);
        dbPaymentIntent.setCurrency(currency);
        dbPaymentIntent.setStatus(paymentIntent.getStatus());
        dbPaymentIntent.setPaymentMethodId(paymentMethodId);
        dbPaymentIntent.setClientSecret(paymentIntent.getClientSecret());
        dbPaymentIntent.setStripeCustomerId(customerId);
        dbPaymentIntent.setSetupFutureUsage("off_session");
        dbPaymentIntent.setPaymentMethodTypes("card");
        dbPaymentIntent.setRequiresAction(paymentIntent.getStatus().equals("requires_action"));
        dbPaymentIntent.setMetadata(String.format("{\"subscription_id\":\"%s\",\"plan_id\":\"%s\"}", 
            stripeSubscription.getId(), planId));
        dbPaymentIntent.setCreatedAt(LocalDateTime.now());
        dbPaymentIntent.setUpdatedAt(LocalDateTime.now());
        paymentIntentRepository.save(dbPaymentIntent);
        logger.trace("Payment intent saved to database - ID: {}", dbPaymentIntent.getId());

        // Create subscription in pending state
        UserSubscription subscription = new UserSubscription();
        subscription.setAccountId(accountId);
        subscription.setPlanId(planId);
        subscription.setStripeCustomerId(customerId);
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStatus(stripeSubscription.getStatus());
        subscription.setActive("active".equals(stripeSubscription.getStatus()));
        subscription.setSubscriptionStartDate(LocalDateTime.now());
        subscription.setCreatedAt(LocalDateTime.now());
        subscription = userSubscriptionRepository.save(subscription);
        logger.trace("New subscription created in pending state - Account: {}, Plan: {}", 
                accountId, planId);

        return SubscriptionUpdateResponse.fromUserSubscription(subscription, 
                paymentIntent.getClientSecret(), true, amount, currency);
    }

    @Transactional
    public SubscriptionUpdateResponse confirmPayment(String paymentIntentId, String subscriptionId) throws StripeException {
        logger.trace("Confirming payment - Payment Intent: {}, Subscription: {}", paymentIntentId, subscriptionId);
        Stripe.apiKey = stripeSecretKey;
        
        // Retrieve the payment intent
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        logger.trace("Retrieved payment intent - Status: {}, Amount: {}", 
                paymentIntent.getStatus(), paymentIntent.getAmount());
        
        if (!"succeeded".equals(paymentIntent.getStatus())) {
            logger.trace("Payment not successful - Current status: {}", paymentIntent.getStatus());
            throw new RuntimeException("Payment not successful. Status: " + paymentIntent.getStatus());
        }
        
        // Find the subscription
        final UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        logger.trace("Found subscription - Current status: {}, Active: {}", 
                subscription.getStatus(), subscription.isActive());
        
        // Get the Stripe subscription and confirm it
        Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
        if ("incomplete".equals(stripeSubscription.getStatus())) {
            logger.trace("Confirming incomplete subscription");
            // For incomplete subscriptions, we can only update certain parameters
            Map<String, Object> params = new HashMap<>();
            params.put("default_payment_method", paymentIntent.getPaymentMethod());
            params.put("metadata", Map.of(
                "payment_intent_id", paymentIntentId,
                "last_payment_date", LocalDateTime.now().toString()
            ));
            
            stripeSubscription = stripeSubscription.update(params);
            logger.trace("Subscription updated with payment method - New status: {}", stripeSubscription.getStatus());
            
            // The subscription should automatically transition to active after successful payment
            // We'll wait for the webhook to handle the status change
            logger.trace("Waiting for webhook to handle subscription activation");
        }
        
        // Get plan details to determine subscription interval
        SubscriptionPlan plan = subscriptionPlanService.getPlanById(subscription.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plan not found: " + subscription.getPlanId()));
        
        // Calculate dates based on Stripe subscription data and plan interval
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now;
        LocalDateTime nextBillingDate = null;
        
        logger.debug("Calculating next billing date for subscription: {} with plan: {}", 
                subscriptionId, plan.getId());
        logger.debug("Plan interval: {}, Plan type: {}", plan.getInterval(), 
                SubscriptionPlanType.fromPlanId(plan.getId()));
        
        // Determine the plan type and calculate next billing date accordingly
        SubscriptionPlanType planType = SubscriptionPlanType.fromPlanId(plan.getId());
        if (planType != null && planType.isAnnual()) {
            nextBillingDate = now.plusYears(1);
            logger.debug("Using annual interval for next billing date: {}", nextBillingDate);
        } else {
            // For monthly plans or if plan type is not found, use monthly interval
            nextBillingDate = now.plusMonths(1);
            logger.debug("Using monthly interval for next billing date: {}", nextBillingDate);
        }
        
        // Update subscription status and dates
        subscription.setStatus(stripeSubscription.getStatus());
        subscription.setActive("active".equals(stripeSubscription.getStatus()));
        subscription.setLastPaymentDate(now);
        subscription.setSubscriptionStartDate(startDate);
        subscription.setNextBillingDate(nextBillingDate);
        
        // If the subscription is set to cancel at period end, set the end date
        if (stripeSubscription.getCancelAtPeriodEnd()) {
            subscription.setSubscriptionEndDate(nextBillingDate);
            subscription.setCancelAtPeriodEnd(true);
            logger.debug("Subscription set to cancel at period end: {}", nextBillingDate);
        } else {
            subscription.setSubscriptionEndDate(null);
            subscription.setCancelAtPeriodEnd(false);
            logger.debug("Subscription not set to cancel at period end");
        }
        
        UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);
        logger.debug("Subscription updated - Start: {}, Next billing: {}, End: {}, Interval: {}", 
                savedSubscription.getSubscriptionStartDate(),
                savedSubscription.getNextBillingDate(),
                savedSubscription.getSubscriptionEndDate(),
                plan.getInterval());
        
        return SubscriptionUpdateResponse.fromUserSubscription(savedSubscription, null, false, 
                plan.getAmount(), plan.getCurrency());
    }

    @Transactional
    public void handleFailedPayment(String paymentIntentId, String subscriptionId, String errorMessage) {
        logger.info("Handling failed payment for subscription: {}, error: {}", subscriptionId, errorMessage);
        
        UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        subscription.setStatus("payment_failed");
        subscription.setActive(false);
        userSubscriptionRepository.save(subscription);
        
        // TODO: Implement notification service to alert user about payment failure
        logger.warn("Payment failed for subscription: {}. Error: {}", subscriptionId, errorMessage);
    }

    @Transactional
    public void handlePaymentRequiresAction(String paymentIntentId, String subscriptionId, String nextAction) {
        logger.info("Payment requires action for subscription: {}, action: {}", subscriptionId, nextAction);
        
        UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        subscription.setStatus("requires_action");
        userSubscriptionRepository.save(subscription);
        
        // TODO: Implement notification service to alert user about required action
        logger.info("Payment requires action for subscription: {}. Action: {}", subscriptionId, nextAction);
    }

    @Transactional
    public void handleSubscriptionCreated(String subscriptionId, String status) {
        logger.info("Handling subscription created: {}, status: {}", subscriptionId, status);
        
        UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        subscription.setStatus(status);
        subscription.setActive("active".equals(status));
        subscription.setSubscriptionStartDate(LocalDateTime.now());
        userSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void handleSubscriptionUpdated(String subscriptionId, String status, Boolean cancelAtPeriodEnd) {
        logger.info("Handling subscription updated: {}, status: {}, cancelAtPeriodEnd: {}", 
                subscriptionId, status, cancelAtPeriodEnd);
        
        UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        subscription.setStatus(status);
        subscription.setActive("active".equals(status));
        subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
        
        if ("canceled".equals(status)) {
            subscription.setSubscriptionEndDate(LocalDateTime.now());
        }
        
        userSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void handleSubscriptionDeleted(String subscriptionId) {
        logger.info("Handling subscription deleted: {}", subscriptionId);
        
        UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        subscription.setStatus("canceled");
        subscription.setActive(false);
        subscription.setSubscriptionEndDate(LocalDateTime.now());
        userSubscriptionRepository.save(subscription);
    }

    private SubscriptionUpdateResponse updateExistingSubscriptionWithPayment(UserSubscription currentSubscription,
            String planId, String stripePriceId, String paymentMethodId, BigDecimal amount, 
            String currency, String returnUrl) throws StripeException {
        
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 3B: Update Existing Paid Subscription");
        logger.trace("║ - Current Plan: {}", currentSubscription.getPlanId());
        logger.trace("║ - New Plan: {}", planId);
        logger.trace("╚══════════════════════════════════════════════════════════════");

        // Update payment method if provided
        String customerId = currentSubscription.getStripeCustomerId();
        logger.trace("║ Updating Payment Method");
        logger.trace("║ - Customer ID: {}", customerId);
        logger.trace("║ - Payment Method: {}", paymentMethodId);

        try {
            Customer customer = Customer.retrieve(customerId);
            customer.update(Map.of("invoice_settings", Map.of("default_payment_method", paymentMethodId)));
            logger.trace("✓ Payment method updated successfully");
        } catch (Exception e) {
            logger.error("❌ Error updating payment method: {}", e.getMessage());
            throw new RuntimeException("Failed to update payment method: " + e.getMessage());
        }

        // STEP 4: Create Payment Intent
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 4: Create Payment Intent");
        logger.trace("╚══════════════════════════════════════════════════════════════");

        Map<String, Object> paymentIntentParams = new HashMap<>();
        int amountInCents = amount.multiply(BigDecimal.valueOf(100)).intValue();
        paymentIntentParams.put("amount", amountInCents);
        paymentIntentParams.put("currency", currency.toLowerCase());
        paymentIntentParams.put("customer", customerId);
        paymentIntentParams.put("payment_method", paymentMethodId);
        paymentIntentParams.put("payment_method_types", Arrays.asList("card"));
        paymentIntentParams.put("setup_future_usage", "off_session");
        paymentIntentParams.put("metadata", Map.of(
            "subscription_id", currentSubscription.getStripeSubscriptionId(),
            "account_id", currentSubscription.getAccountId().toString()
        ));

        logger.trace("║ Payment Intent Parameters:");
        logger.trace("║ - Amount: {} cents", amountInCents);
        logger.trace("║ - Currency: {}", currency);
        logger.trace("║ - Setup Future Usage: {}", paymentIntentParams.get("setup_future_usage"));

        PaymentIntent paymentIntent = PaymentIntent.create(paymentIntentParams);
        logger.trace("✓ Payment Intent created");
        logger.trace("║ - ID: {}", paymentIntent.getId());
        logger.trace("║ - Status: {}", paymentIntent.getStatus());

        // STEP 5: Update Database
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 5: Update Local Database");
        logger.trace("╚══════════════════════════════════════════════════════════════");

        currentSubscription.setPlanId(planId);
        currentSubscription.setStatus("pending_payment");
        currentSubscription = userSubscriptionRepository.save(currentSubscription);
        logger.trace("✓ Database updated");
        logger.trace("║ - New Status: {}", currentSubscription.getStatus());
        logger.trace("║ - New Plan ID: {}", currentSubscription.getPlanId());

        // STEP 6: Return Response
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 6: Return Client Secret");
        logger.trace("║ Payment flow ready for frontend confirmation");
        logger.trace("╚══════════════════════════════════════════════════════════════");

        return SubscriptionUpdateResponse.fromUserSubscription(currentSubscription, 
                paymentIntent.getClientSecret(), true, amount, currency);
    }

    @Transactional
    public SubscriptionUpdateResponse cancelSubscription(String subscriptionId) throws StripeException {
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 1: Subscription Cancellation Initiation");
        logger.trace("║ Subscription ID: {}", subscriptionId);
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
        Stripe.apiKey = stripeSecretKey;
        
        // Find the subscription
        final UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 2: Subscription Details Retrieved");
        logger.trace("║ - Status: {}", subscription.getStatus());
        logger.trace("║ - Active: {}", subscription.isActive());
        logger.trace("║ - Plan ID: {}", subscription.getPlanId());
        logger.trace("║ - Stripe Subscription ID: {}", subscription.getStripeSubscriptionId());
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
        // Check if it's a free subscription
        if (subscription.getStripeSubscriptionId().startsWith("free_")) {
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 3: Free Subscription Cancellation Attempt");
            logger.trace("║ ❌ Error: Cannot cancel free subscription");
            logger.trace("╚══════════════════════════════════════════════════════════════");
            throw new RuntimeException("Cannot cancel free subscription. Please upgrade to a paid plan first.");
        }
        
        // For paid subscriptions, cancel in Stripe
        try {
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 3: Retrieving Stripe Subscription");
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 4: Preparing Stripe Cancellation");
            logger.trace("║ - Current Status: {}", stripeSubscription.getStatus());
            logger.trace("║ - Cancel at Period End: {}", stripeSubscription.getCancelAtPeriodEnd());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            // Cancel at period end instead of immediately
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(true)
                    .build();
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 5: Updating Stripe Subscription");
            logger.trace("║ - Setting cancel_at_period_end to true");
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            stripeSubscription = stripeSubscription.update(params);
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 6: Stripe Update Complete");
            logger.trace("║ - New Status: {}", stripeSubscription.getStatus());
            logger.trace("║ - Cancel at Period End: {}", stripeSubscription.getCancelAtPeriodEnd());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            // Update our database
            subscription.setCancelAtPeriodEnd(true);
            
            // Get the current period end from the subscription items
            if (stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
                Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
                if (periodEnd != null) {
                    LocalDateTime endDate = LocalDateTime.ofEpochSecond(periodEnd, 0, ZoneOffset.UTC);
                    subscription.setSubscriptionEndDate(endDate);
                    logger.trace("║ - Period End Date: {}", endDate);
                }
            }
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 7: Updating Local Database");
            logger.trace("║ - Setting cancel_at_period_end to true");
            logger.trace("║ - Updating subscription end date");
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 8: Database Update Complete");
            logger.trace("║ - Status: {}", savedSubscription.getStatus());
            logger.trace("║ - Active: {}", savedSubscription.isActive());
            logger.trace("║ - End Date: {}", savedSubscription.getSubscriptionEndDate());
            logger.trace("║ - Cancel at Period End: {}", savedSubscription.getCancelAtPeriodEnd());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            // Get plan details for the response
            SubscriptionPlan plan = subscriptionPlanService.getPlanById(savedSubscription.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan not found: " + savedSubscription.getPlanId()));
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 9: Preparing Response");
            logger.trace("║ - Plan ID: {}", plan.getId());
            logger.trace("║ - Amount: {}", plan.getAmount());
            logger.trace("║ - Currency: {}", plan.getCurrency());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            return SubscriptionUpdateResponse.fromUserSubscription(savedSubscription, null, false, 
                    plan.getAmount(), plan.getCurrency());
            
        } catch (StripeException e) {
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ ❌ ERROR: Stripe Operation Failed");
            logger.trace("║ - Error Message: {}", e.getMessage());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            throw new RuntimeException("Failed to cancel subscription: " + e.getMessage());
        }
    }

    @Transactional
    public SubscriptionUpdateResponse reactivateSubscription(String subscriptionId) throws StripeException {
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 1: Subscription Reactivation Initiation");
        logger.trace("║ Subscription ID: {}", subscriptionId);
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
        Stripe.apiKey = stripeSecretKey;
        
        // Find the subscription
        final UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 2: Subscription Details Retrieved");
        logger.trace("║ - Status: {}", subscription.getStatus());
        logger.trace("║ - Active: {}", subscription.isActive());
        logger.trace("║ - Plan ID: {}", subscription.getPlanId());
        logger.trace("║ - Cancel at Period End: {}", subscription.getCancelAtPeriodEnd());
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
        // Check if it's a free subscription
        if (subscription.getStripeSubscriptionId().startsWith("free_")) {
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 3: Free Subscription Reactivation Attempt");
            logger.trace("║ ❌ Error: Cannot reactivate free subscription");
            logger.trace("╚══════════════════════════════════════════════════════════════");
            throw new RuntimeException("Cannot reactivate free subscription. Please upgrade to a paid plan first.");
        }
        
        // Check if subscription is already active and not set to cancel
        if (subscription.isActive() && !subscription.getCancelAtPeriodEnd()) {
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 3: Subscription Status Check");
            logger.trace("║ ❌ Error: Subscription is already active and not set to cancel");
            logger.trace("╚══════════════════════════════════════════════════════════════");
            throw new RuntimeException("Subscription is already active and not set to cancel");
        }
        
        try {
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 3: Retrieving Stripe Subscription");
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 4: Preparing Stripe Reactivation");
            logger.trace("║ - Current Status: {}", stripeSubscription.getStatus());
            logger.trace("║ - Cancel at Period End: {}", stripeSubscription.getCancelAtPeriodEnd());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            // Reactivate by setting cancel_at_period_end to false
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(false)
                    .build();
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 5: Updating Stripe Subscription");
            logger.trace("║ - Setting cancel_at_period_end to false");
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            stripeSubscription = stripeSubscription.update(params);
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 6: Stripe Update Complete");
            logger.trace("║ - New Status: {}", stripeSubscription.getStatus());
            logger.trace("║ - Cancel at Period End: {}", stripeSubscription.getCancelAtPeriodEnd());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            // Update our database
            subscription.setCancelAtPeriodEnd(false);
            subscription.setSubscriptionEndDate(null); // Clear the end date since it's reactivated
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 7: Updating Local Database");
            logger.trace("║ - Setting cancel_at_period_end to false");
            logger.trace("║ - Clearing subscription end date");
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 8: Database Update Complete");
            logger.trace("║ - Status: {}", savedSubscription.getStatus());
            logger.trace("║ - Active: {}", savedSubscription.isActive());
            logger.trace("║ - End Date: {}", savedSubscription.getSubscriptionEndDate());
            logger.trace("║ - Cancel at Period End: {}", savedSubscription.getCancelAtPeriodEnd());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            // Get plan details for the response
            SubscriptionPlan plan = subscriptionPlanService.getPlanById(savedSubscription.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Plan not found: " + savedSubscription.getPlanId()));
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 9: Preparing Response");
            logger.trace("║ - Plan ID: {}", plan.getId());
            logger.trace("║ - Amount: {}", plan.getAmount());
            logger.trace("║ - Currency: {}", plan.getCurrency());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            
            return SubscriptionUpdateResponse.fromUserSubscription(savedSubscription, null, false, 
                    plan.getAmount(), plan.getCurrency());
            
        } catch (StripeException e) {
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ ❌ ERROR: Stripe Operation Failed");
            logger.trace("║ - Error Message: {}", e.getMessage());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            throw new RuntimeException("Failed to reactivate subscription: " + e.getMessage());
        }
    }

    @Transactional
    public SubscriptionUpdateResponse downgradeSubscriptionWithPayment(UUID accountId, String planId, 
            String paymentMethodId, String returnUrl) throws StripeException {
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 1: Subscription Downgrade Initiation");
        logger.trace("║ Account: {}", accountId);
        logger.trace("║ Plan ID: {}", planId);
        logger.trace("║ Payment Method: {}", paymentMethodId);
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
        // Get plan details
        SubscriptionPlan newPlan = subscriptionPlanService.getPlanById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));
        
        String stripePriceId = newPlan.getStripePriceId();
        
        logger.trace("║ Plan Details Retrieved:");
        logger.trace("║ - Plan ID: {}", planId);
        logger.trace("║ - Stripe Price ID: {}", stripePriceId);
        logger.trace("║ - Amount: {}", newPlan.getAmount());
        logger.trace("║ - Currency: {}", newPlan.getCurrency());

        // Get current subscription
        Optional<UserSubscription> currentSubscription = userSubscriptionRepository.findByAccountId(accountId);
        logger.trace("║ Current Subscription Status:");
        logger.trace("║ - Exists: {}", currentSubscription.isPresent());
        if (currentSubscription.isPresent()) {
            logger.trace("║ - Current Plan: {}", currentSubscription.get().getPlanId());
            logger.trace("║ - Status: {}", currentSubscription.get().getStatus());
        }

        if (currentSubscription.isEmpty()) {
            throw new RuntimeException("No active subscription found for account: " + accountId);
        }

        UserSubscription subscription = currentSubscription.get();

        // Verify it's not a free subscription
        if (subscription.getStripeSubscriptionId().startsWith("free_")) {
            throw new RuntimeException("Cannot downgrade free subscription");
        }

        // Get current plan details
        SubscriptionPlan currentPlan = subscriptionPlanService.getPlanById(subscription.getPlanId())
                .orElseThrow(() -> new RuntimeException("Current plan not found: " + subscription.getPlanId()));

        // Verify it's actually a downgrade
        if (newPlan.getAmount().compareTo(currentPlan.getAmount()) >= 0) {
            throw new RuntimeException("Cannot downgrade to a plan with equal or higher price");
        }

        // Configure Stripe API
        Stripe.apiKey = stripeSecretKey;

        try {
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 2: Retrieving Stripe Subscription");
            logger.trace("╚══════════════════════════════════════════════════════════════");

            Subscription stripeSubscription = Subscription.retrieve(subscription.getStripeSubscriptionId());

            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 3: Preparing Stripe Downgrade");
            logger.trace("║ - Current Status: {}", stripeSubscription.getStatus());
            logger.trace("║ - Current Price ID: {}", stripeSubscription.getItems().getData().get(0).getPrice().getId());
            logger.trace("║ - New Price ID: {}", stripePriceId);
            logger.trace("╚══════════════════════════════════════════════════════════════");

            // Update subscription in Stripe to apply changes at period end
            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(stripeSubscription.getItems().getData().get(0).getId())
                            .setPrice(stripePriceId)
                            .build())
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.NONE)
                    .setBillingCycleAnchor(SubscriptionUpdateParams.BillingCycleAnchor.NOW)
                    .setCancelAtPeriodEnd(false)
                    .build();

            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 4: Updating Stripe Subscription");
            logger.trace("║ - Setting new price ID");
            logger.trace("║ - Applying changes at period end");
            logger.trace("║ - No prorations");
            logger.trace("╚══════════════════════════════════════════════════════════════");

            stripeSubscription = stripeSubscription.update(params);

            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 5: Stripe Update Complete");
            logger.trace("║ - New Status: {}", stripeSubscription.getStatus());
            logger.trace("║ - New Price ID: {}", stripeSubscription.getItems().getData().get(0).getPrice().getId());
            logger.trace("║ - Next Billing Date: {}", stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd());
            logger.trace("╚══════════════════════════════════════════════════════════════");

            // Update our database
            subscription.setPlanId(planId);
            subscription.setStatus(stripeSubscription.getStatus());
            subscription.setActive("active".equals(stripeSubscription.getStatus()));
            subscription.setPendingPlanChange(true); // Mark that there's a pending plan change

            // Calculate next billing date
            if (stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
                Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
                if (periodEnd != null) {
                    subscription.setNextBillingDate(LocalDateTime.ofEpochSecond(periodEnd, 0, ZoneOffset.UTC));
                }
            }

            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 6: Updating Local Database");
            logger.trace("║ - New Plan ID: {}", subscription.getPlanId());
            logger.trace("║ - New Status: {}", subscription.getStatus());
            logger.trace("║ - Next Billing Date: {}", subscription.getNextBillingDate());
            logger.trace("║ - Pending Plan Change: {}", subscription.getPendingPlanChange());
            logger.trace("╚══════════════════════════════════════════════════════════════");

            UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);

            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 7: Database Update Complete");
            logger.trace("║ - Status: {}", savedSubscription.getStatus());
            logger.trace("║ - Active: {}", savedSubscription.isActive());
            logger.trace("║ - Plan ID: {}", savedSubscription.getPlanId());
            logger.trace("║ - Pending Plan Change: {}", savedSubscription.getPendingPlanChange());
            logger.trace("╚══════════════════════════════════════════════════════════════");

            return SubscriptionUpdateResponse.fromUserSubscription(savedSubscription, null, false, 
                    newPlan.getAmount(), newPlan.getCurrency());

        } catch (StripeException e) {
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ ❌ ERROR: Stripe Operation Failed");
            logger.trace("║ - Error Message: {}", e.getMessage());
            logger.trace("╚══════════════════════════════════════════════════════════════");
            throw new RuntimeException("Failed to downgrade subscription: " + e.getMessage());
        }
    }
}