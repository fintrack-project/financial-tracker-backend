package com.fintrack.service.subscription;

import com.fintrack.constants.subscription.SubscriptionPlanType;
import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.repository.payment.PaymentIntentRepository;
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

@Service
public class UserSubscriptionUpgradeService extends BaseUserSubscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionUpgradeService.class);
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public UserSubscriptionUpgradeService(
            UserSubscriptionRepository userSubscriptionRepository,
            PaymentService paymentService,
            SubscriptionPlanService subscriptionPlanService,
            PaymentIntentRepository paymentIntentRepository) {
        super(userSubscriptionRepository, paymentService, subscriptionPlanService, paymentIntentRepository);
    }

    @Transactional
    public SubscriptionUpdateResponse upgradeSubscription(UUID accountId, String planId, 
            String paymentMethodId, String returnUrl) throws StripeException {
        
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 1: Subscription Upgrade Initiation");
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

        // Get current subscription
        UserSubscription currentSubscription = userSubscriptionRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("No subscription found for account: " + accountId));
        validateSubscriptionExists(currentSubscription, currentSubscription.getStripeSubscriptionId());

        // Validate upgrade
        validateUpgrade(currentSubscription, plan);

        // Configure Stripe API
        Stripe.apiKey = stripeSecretKey;

        // Handle free to paid upgrade
        if (currentSubscription.getStripeSubscriptionId().startsWith("free_")) {
            return handleFreeToPaidUpgrade(currentSubscription, plan, paymentMethodId, returnUrl);
        }

        // Update existing paid subscription
        return updatePaidSubscription(currentSubscription, plan, paymentMethodId, returnUrl);
    }

    private void validateUpgrade(UserSubscription currentSubscription, SubscriptionPlan newPlan) {
        SubscriptionPlan currentPlan = subscriptionPlanService.getPlanById(currentSubscription.getPlanId())
                .orElseThrow(() -> new RuntimeException("Current plan not found: " + currentSubscription.getPlanId()));

        if (newPlan.getAmount().compareTo(currentPlan.getAmount()) <= 0) {
            throw new RuntimeException("New plan must have a higher price than current plan");
        }
    }

    private SubscriptionUpdateResponse handleFreeToPaidUpgrade(UserSubscription currentSubscription, 
            SubscriptionPlan newPlan, String paymentMethodId, String returnUrl) throws StripeException {
        
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 2: Handling Free to Paid Upgrade");
        logger.trace("║ - Current Plan: {}", currentSubscription.getPlanId());
        logger.trace("║ - New Plan: {}", newPlan.getId());
        logger.trace("╚══════════════════════════════════════════════════════════════");

        // Create new Stripe subscription
        String customerId = ensureStripeCustomerExists(currentSubscription.getAccountId());
        
        // Create subscription in Stripe
        SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(newPlan.getStripePriceId())
                        .build())
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.DEFAULT_INCOMPLETE)
                .setPaymentSettings(SubscriptionCreateParams.PaymentSettings.builder()
                        .setPaymentMethodTypes(List.of(SubscriptionCreateParams.PaymentSettings.PaymentMethodType.CARD))
                        .setSaveDefaultPaymentMethod(SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                        .build())
                .addExpand("latest_invoice")
                .setMetadata(Map.of(
                    "account_id", currentSubscription.getAccountId().toString(),
                    "plan_id", newPlan.getId(),
                    "subscription_type", SubscriptionPlanType.fromPlanId(newPlan.getId()).name().toLowerCase(),
                    "upgrade_from", "free"
                ));

        Subscription stripeSubscription = Subscription.create(paramsBuilder.build());
        logger.trace("✓ Stripe subscription created");
        logger.trace("║ - Subscription ID: {}", stripeSubscription.getId());
        logger.trace("║ - Status: {}", stripeSubscription.getStatus());

        // Create payment intent
        Map<String, Object> paymentIntentParams = new HashMap<>();
        int amountInCents = newPlan.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
        paymentIntentParams.put("amount", amountInCents);
        paymentIntentParams.put("currency", newPlan.getCurrency().toLowerCase());
        paymentIntentParams.put("customer", customerId);
        // Don't set payment method - let frontend handle it
        // paymentIntentParams.put("payment_method", paymentMethodId);
        paymentIntentParams.put("payment_method_types", Arrays.asList("card"));
        paymentIntentParams.put("setup_future_usage", "off_session");
        paymentIntentParams.put("confirm", false);  // Never confirm from backend
        
        // Only use return_url if explicitly provided by frontend
        if (returnUrl != null && !returnUrl.isEmpty()) {
            paymentIntentParams.put("confirm", true);
            paymentIntentParams.put("return_url", returnUrl);
        }
        
        paymentIntentParams.put("metadata", Map.of(
            "subscription_id", stripeSubscription.getId(),
            "account_id", currentSubscription.getAccountId().toString(),
            "plan_id", newPlan.getId(),
            "subscription_type", SubscriptionPlanType.fromPlanId(newPlan.getId()).name().toLowerCase(),
            "upgrade_from", "free"
        ));

        PaymentIntent stripePaymentIntent = PaymentIntent.create(paymentIntentParams);
        logger.trace("✓ Payment Intent created");
        logger.trace("║ - ID: {}", stripePaymentIntent.getId());
        logger.trace("║ - Status: {}", stripePaymentIntent.getStatus());
        logger.trace("║ - Amount: {}", stripePaymentIntent.getAmount());
        logger.trace("║ - Currency: {}", stripePaymentIntent.getCurrency());
        logger.trace("║ - Payment Method Types: {}", stripePaymentIntent.getPaymentMethodTypes());
        logger.trace("║ - Client Secret: {}", stripePaymentIntent.getClientSecret());
        logger.trace("║ - Requires Action: {}", stripePaymentIntent.getStatus().equals("requires_action"));
        logger.trace("║ - Requires Confirmation: {}", stripePaymentIntent.getStatus().equals("requires_confirmation"));

        // Save payment intent to our database
        com.fintrack.model.payment.PaymentIntent dbPaymentIntent = new com.fintrack.model.payment.PaymentIntent();
        dbPaymentIntent.setAccountId(currentSubscription.getAccountId());
        dbPaymentIntent.setStripePaymentIntentId(stripePaymentIntent.getId());
        dbPaymentIntent.setAmount(newPlan.getAmount());
        dbPaymentIntent.setCurrency(newPlan.getCurrency());
        dbPaymentIntent.setStatus(stripePaymentIntent.getStatus());
        dbPaymentIntent.setPaymentMethodId(paymentMethodId);
        dbPaymentIntent.setClientSecret(stripePaymentIntent.getClientSecret());
        dbPaymentIntent.setStripeCustomerId(customerId);
        dbPaymentIntent.setSetupFutureUsage("off_session");
        dbPaymentIntent.setPaymentMethodTypes("card");
        dbPaymentIntent.setRequiresAction(stripePaymentIntent.getStatus().equals("requires_action"));
        dbPaymentIntent.setMetadata(String.format("{\"subscription_id\":\"%s\",\"plan_id\":\"%s\"}", 
            stripeSubscription.getId(), newPlan.getId()));
        dbPaymentIntent.setCreatedAt(LocalDateTime.now());
        paymentIntentRepository.save(dbPaymentIntent);
        logger.trace("✓ Payment intent saved to database");
        
        // Update subscription in database
        currentSubscription.setStripeSubscriptionId(stripeSubscription.getId());
        currentSubscription.setPlanId(newPlan.getId());
        currentSubscription.setStatus("pending_payment");
        currentSubscription.setActive(false);
        currentSubscription.setLastPaymentDate(LocalDateTime.now());
        currentSubscription = userSubscriptionRepository.save(currentSubscription);

        return SubscriptionUpdateResponse.fromUserSubscription(currentSubscription, 
                stripePaymentIntent.getClientSecret(), true, newPlan.getAmount(), newPlan.getCurrency());
    }

    private SubscriptionUpdateResponse updatePaidSubscription(UserSubscription currentSubscription, 
            SubscriptionPlan newPlan, String paymentMethodId, String returnUrl) throws StripeException {
        
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 2: Updating Paid Subscription");
        logger.trace("║ - Current Plan: {}", currentSubscription.getPlanId());
        logger.trace("║ - New Plan: {}", newPlan.getId());
        logger.trace("╚══════════════════════════════════════════════════════════════");

        try {
            // Update subscription in Stripe
            Subscription stripeSubscription = Subscription.retrieve(currentSubscription.getStripeSubscriptionId());
            String subscriptionItemId = stripeSubscription.getItems().getData().get(0).getId();

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(subscriptionItemId)
                            .setPrice(newPlan.getStripePriceId())
                            .build())
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE)
                    .setMetadata(Map.of(
                        "plan_id", newPlan.getId(),
                        "subscription_type", SubscriptionPlanType.fromPlanId(newPlan.getId()).name().toLowerCase(),
                        "upgrade_from", SubscriptionPlanType.fromPlanId(currentSubscription.getPlanId()).name().toLowerCase()
                    ))
                    .build();

            stripeSubscription = stripeSubscription.update(params);
            logger.trace("✓ Stripe subscription updated");
            logger.trace("║ - New Status: {}", stripeSubscription.getStatus());

            // Create payment intent for the price difference
            Map<String, Object> paymentIntentParams = new HashMap<>();
            int amountInCents = newPlan.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
            paymentIntentParams.put("amount", amountInCents);
            paymentIntentParams.put("currency", newPlan.getCurrency().toLowerCase());
            paymentIntentParams.put("customer", currentSubscription.getStripeCustomerId());
            // Don't set payment method - let frontend handle it
            // paymentIntentParams.put("payment_method", paymentMethodId);
            paymentIntentParams.put("payment_method_types", Arrays.asList("card"));
            paymentIntentParams.put("setup_future_usage", "off_session");
            paymentIntentParams.put("confirm", false);  // Never confirm from backend
            
            // Only use return_url if explicitly provided by frontend
            if (returnUrl != null && !returnUrl.isEmpty()) {
                paymentIntentParams.put("confirm", true);
                paymentIntentParams.put("return_url", returnUrl);
            }
            
            paymentIntentParams.put("metadata", Map.of(
                "subscription_id", stripeSubscription.getId(),
                "account_id", currentSubscription.getAccountId().toString(),
                "plan_id", newPlan.getId(),
                "subscription_type", SubscriptionPlanType.fromPlanId(newPlan.getId()).name().toLowerCase(),
                "upgrade_from", SubscriptionPlanType.fromPlanId(currentSubscription.getPlanId()).name().toLowerCase()
            ));

            PaymentIntent stripePaymentIntent = PaymentIntent.create(paymentIntentParams);
            logger.trace("✓ Payment Intent created");
            logger.trace("║ - ID: {}", stripePaymentIntent.getId());
            logger.trace("║ - Status: {}", stripePaymentIntent.getStatus());
            logger.trace("║ - Amount: {}", stripePaymentIntent.getAmount());
            logger.trace("║ - Currency: {}", stripePaymentIntent.getCurrency());
            logger.trace("║ - Payment Method Types: {}", stripePaymentIntent.getPaymentMethodTypes());
            logger.trace("║ - Client Secret: {}", stripePaymentIntent.getClientSecret());
            logger.trace("║ - Requires Action: {}", stripePaymentIntent.getStatus().equals("requires_action"));
            logger.trace("║ - Requires Confirmation: {}", stripePaymentIntent.getStatus().equals("requires_confirmation"));

            // Save payment intent to our database
            com.fintrack.model.payment.PaymentIntent dbPaymentIntent = new com.fintrack.model.payment.PaymentIntent();
            dbPaymentIntent.setAccountId(currentSubscription.getAccountId());
            dbPaymentIntent.setStripePaymentIntentId(stripePaymentIntent.getId());
            dbPaymentIntent.setAmount(newPlan.getAmount());
            dbPaymentIntent.setCurrency(newPlan.getCurrency());
            dbPaymentIntent.setStatus(stripePaymentIntent.getStatus());
            dbPaymentIntent.setPaymentMethodId(paymentMethodId);
            dbPaymentIntent.setClientSecret(stripePaymentIntent.getClientSecret());
            dbPaymentIntent.setStripeCustomerId(currentSubscription.getStripeCustomerId());
            dbPaymentIntent.setSetupFutureUsage("off_session");
            dbPaymentIntent.setPaymentMethodTypes("card");
            dbPaymentIntent.setRequiresAction(stripePaymentIntent.getStatus().equals("requires_action"));
            dbPaymentIntent.setMetadata(String.format("{\"subscription_id\":\"%s\",\"plan_id\":\"%s\"}", 
                stripeSubscription.getId(), newPlan.getId()));
            dbPaymentIntent.setCreatedAt(LocalDateTime.now());
            paymentIntentRepository.save(dbPaymentIntent);
            logger.trace("✓ Payment intent saved to database");

            // Calculate next billing date
            LocalDateTime nextBillingDate;
            if (stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
                Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
                if (periodEnd != null) {
                    nextBillingDate = LocalDateTime.ofEpochSecond(periodEnd, 0, ZoneOffset.UTC);
                } else {
                    nextBillingDate = LocalDateTime.now().plusDays(30);
                }
            } else {
                nextBillingDate = LocalDateTime.now().plusDays(30);
            }

            // Update subscription in database
            currentSubscription.setPlanId(newPlan.getId());
            currentSubscription.setStatus(stripeSubscription.getStatus());
            currentSubscription.setActive("active".equals(stripeSubscription.getStatus()));
            currentSubscription.setNextBillingDate(nextBillingDate);
            currentSubscription.setLastPaymentDate(LocalDateTime.now());
            currentSubscription = userSubscriptionRepository.save(currentSubscription);

            logger.trace("✓ Database updated");
            logger.trace("║ - New Status: {}", currentSubscription.getStatus());
            logger.trace("║ - Next Billing Date: {}", currentSubscription.getNextBillingDate());

            return SubscriptionUpdateResponse.fromUserSubscription(currentSubscription, 
                    stripePaymentIntent.getClientSecret(), true, newPlan.getAmount(), newPlan.getCurrency());

        } catch (StripeException e) {
            logger.error("❌ Error updating subscription: {}", e.getMessage());
            throw new RuntimeException("Failed to update subscription: " + e.getMessage());
        }
    }
} 