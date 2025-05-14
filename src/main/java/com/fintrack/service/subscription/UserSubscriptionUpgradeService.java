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

import com.stripe.model.StripeObject;
import com.google.gson.JsonObject;

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
                        .setPaymentMethodOptions(SubscriptionCreateParams.PaymentSettings.PaymentMethodOptions.builder()
                            .setCard(SubscriptionCreateParams.PaymentSettings.PaymentMethodOptions.Card.builder()
                                .setRequestThreeDSecure(SubscriptionCreateParams.PaymentSettings.PaymentMethodOptions.Card.RequestThreeDSecure.AUTOMATIC)
                                .build())
                            .build())
                        .build())
                .setDefaultPaymentMethod(paymentMethodId)
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

        return handleSubscriptionPayment(currentSubscription, newPlan, paymentMethodId, returnUrl, 
            stripeSubscription, customerId, "free");
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

            return handleSubscriptionPayment(currentSubscription, newPlan, paymentMethodId, returnUrl, 
                stripeSubscription, currentSubscription.getStripeCustomerId(), 
                SubscriptionPlanType.fromPlanId(currentSubscription.getPlanId()).name().toLowerCase());

        } catch (StripeException e) {
            logger.error("❌ Error updating subscription: {}", e.getMessage());
            throw new RuntimeException("Failed to update subscription: " + e.getMessage());
        }
    }

    private SubscriptionUpdateResponse handleSubscriptionPayment(
            UserSubscription currentSubscription,
            SubscriptionPlan newPlan,
            String paymentMethodId,
            String returnUrl,
            Subscription stripeSubscription,
            String customerId,
            String upgradeFrom) throws StripeException {
        
        // Get the latest invoice and its payment intent
        com.stripe.model.PaymentIntent stripePaymentIntent = null;
        
        // For DEFAULT_INCOMPLETE subscriptions, Stripe will create a payment intent
        // and attach it to the latest invoice
        if (stripeSubscription.getLatestInvoice() != null) {
            String invoiceId = stripeSubscription.getLatestInvoice();
            
            // Retry a few times as it might take a moment for Stripe to attach the payment intent
            int maxRetries = 5;  // Increased retries
            int retryCount = 0;
            while (retryCount < maxRetries) {
                com.stripe.model.Invoice invoice = com.stripe.model.Invoice.retrieve(invoiceId);
                logger.trace("║ - Invoice Status: {}", invoice.getStatus());
                
                // Get payment intent from raw JSON
                JsonObject rawJson = invoice.getRawJsonObject();
                if (rawJson.has("payment_intent")) {
                    String paymentIntentId = rawJson.get("payment_intent").getAsString();
                    stripePaymentIntent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
                    logger.trace("✓ Payment Intent retrieved from latest invoice");
                    logger.trace("║ - ID: {}", stripePaymentIntent.getId());
                    logger.trace("║ - Status: {}", stripePaymentIntent.getStatus());
                    logger.trace("║ - Amount: {}", stripePaymentIntent.getAmount());
                    logger.trace("║ - Currency: {}", stripePaymentIntent.getCurrency());
                    logger.trace("║ - Client Secret: {}", stripePaymentIntent.getClientSecret());
                    break;
                }
                
                // Wait a bit before retrying
                try {
                    Thread.sleep(2000); // Increased wait time to 2 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for payment intent", e);
                }
                retryCount++;
            }
        }
        
        if (stripePaymentIntent == null) {
            // If we still don't have a payment intent, try to get it from the subscription's raw JSON
            try {
                JsonObject subscriptionJson = stripeSubscription.getRawJsonObject();
                if (subscriptionJson.has("latest_payment_intent")) {
                    String paymentIntentId = subscriptionJson.get("latest_payment_intent").getAsString();
                    stripePaymentIntent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
                    logger.trace("✓ Payment Intent retrieved from subscription JSON");
                } else {
                    throw new RuntimeException("No payment intent found in subscription JSON");
                }
            } catch (Exception e) {
                logger.error("Failed to get payment intent from subscription: {}", e.getMessage());
                throw new RuntimeException("No payment intent found in latest invoice. This indicates the subscription creation process was not completed properly.");
            }
        }

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
        
        // Calculate next billing date for paid subscriptions
        if (!upgradeFrom.equals("free")) {
            if (stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
                Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
                if (periodEnd != null) {
                    currentSubscription.setNextBillingDate(LocalDateTime.ofEpochSecond(periodEnd, 0, ZoneOffset.UTC));
                } else {
                    currentSubscription.setNextBillingDate(LocalDateTime.now().plusDays(30));
                }
            } else {
                currentSubscription.setNextBillingDate(LocalDateTime.now().plusDays(30));
            }
        }
        
        currentSubscription = userSubscriptionRepository.save(currentSubscription);

        return SubscriptionUpdateResponse.fromUserSubscription(currentSubscription, 
                stripePaymentIntent.getClientSecret(), true, newPlan.getAmount(), newPlan.getCurrency());
    }
} 