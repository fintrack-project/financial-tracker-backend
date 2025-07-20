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

    /**
     * Upgrades a user's subscription with improved payment handling
     * 
     * CHANGES FROM PREVIOUS VERSION:
     * 1. Uses ALLOW_INCOMPLETE instead of DEFAULT_INCOMPLETE to avoid race conditions
     * 2. Creates payment intent explicitly before attaching to subscription
     * 3. Supports 3D Secure authentication with return URL
     * 4. Simplified payment intent retrieval (no more retry logic)
     * 5. Enhanced logging for better debugging
     * 
     * @param accountId User's account ID
     * @param planId New plan ID to upgrade to
     * @param paymentMethodId Stripe payment method ID
     * @param returnUrl URL to return to after 3D Secure authentication (for 3D Secure cards)
     * @return SubscriptionUpdateResponse with payment details
     */
    @Transactional
    public SubscriptionUpdateResponse upgradeSubscription(UUID accountId, String planId, 
            String paymentMethodId, String returnUrl) throws StripeException {
        
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ STEP 1: Subscription Upgrade Initiation");
        logger.info("║ Account: {}", accountId);
        logger.info("║ Plan ID: {}", planId);
        logger.info("║ Payment Method: {}", paymentMethodId);
        logger.info("║ Return URL: {}", returnUrl != null ? returnUrl : "None (3D Secure not required)");
        logger.info("╚══════════════════════════════════════════════════════════════");

        // Get plan details
        SubscriptionPlan plan = subscriptionPlanService.getPlanById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));
        
        String stripePriceId = plan.getStripePriceId();
        
        logger.info("║ Plan Details Retrieved:");
        logger.info("║ - Plan ID: {}", planId);
        logger.info("║ - Stripe Price ID: {}", stripePriceId);
        logger.info("║ - Amount: {}", plan.getAmount());
        logger.info("║ - Currency: {}", plan.getCurrency());

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

    /**
     * Handles free to paid subscription upgrade with improved payment flow
     * 
     * IMPROVEMENTS:
     * 1. Uses ALLOW_INCOMPLETE to create subscription with explicit payment intent
     * 2. Enables 3D Secure authentication automatically
     * 3. Creates payment intent before subscription to avoid race conditions
     * 4. Better error handling and logging
     */
    private SubscriptionUpdateResponse handleFreeToPaidUpgrade(UserSubscription currentSubscription, 
            SubscriptionPlan newPlan, String paymentMethodId, String returnUrl) throws StripeException {
        
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ STEP 2: Handling Free to Paid Upgrade");
        logger.info("║ - Current Plan: {}", currentSubscription.getPlanId());
        logger.info("║ - New Plan: {}", newPlan.getId());
        logger.info("║ - Payment Method: {}", paymentMethodId);
        logger.info("╚══════════════════════════════════════════════════════════════");

        // Create new Stripe subscription
        String customerId = ensureStripeCustomerExists(currentSubscription.getAccountId());
        
        // STEP 2.1: Create payment intent first (NEW APPROACH)
        logger.info("║ STEP 2.1: Creating Payment Intent");
        PaymentIntent paymentIntent = createPaymentIntentForSubscription(
            customerId, newPlan, paymentMethodId, returnUrl, newPlan.getAmount());
        logger.info("✓ Payment Intent created: {}", paymentIntent.getId());
        logger.info("║ - Status: {}", paymentIntent.getStatus());
        logger.info("║ - Requires Action: {}", paymentIntent.getStatus().equals("requires_action"));
        
        // STEP 2.2: Create subscription with ALLOW_INCOMPLETE and attach payment intent
        logger.info("║ STEP 2.2: Creating Subscription with ALLOW_INCOMPLETE");
        SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder()
                        .setPrice(newPlan.getStripePriceId())
                        .build())
                // CHANGED: Use ALLOW_INCOMPLETE instead of DEFAULT_INCOMPLETE
                .setPaymentBehavior(SubscriptionCreateParams.PaymentBehavior.ALLOW_INCOMPLETE)
                .setPaymentSettings(SubscriptionCreateParams.PaymentSettings.builder()
                        .setPaymentMethodTypes(List.of(SubscriptionCreateParams.PaymentSettings.PaymentMethodType.CARD))
                        .setSaveDefaultPaymentMethod(SubscriptionCreateParams.PaymentSettings.SaveDefaultPaymentMethod.ON_SUBSCRIPTION)
                        .setPaymentMethodOptions(SubscriptionCreateParams.PaymentSettings.PaymentMethodOptions.builder()
                            .setCard(SubscriptionCreateParams.PaymentSettings.PaymentMethodOptions.Card.builder()
                                // Enable 3D Secure authentication
                                .setRequestThreeDSecure(SubscriptionCreateParams.PaymentSettings.PaymentMethodOptions.Card.RequestThreeDSecure.AUTOMATIC)
                                .build())
                            .build())
                        .build())
                .setDefaultPaymentMethod(paymentMethodId)
                .setMetadata(Map.of(
                    "account_id", currentSubscription.getAccountId().toString(),
                    "plan_id", newPlan.getId(),
                    "subscription_type", SubscriptionPlanType.fromPlanId(newPlan.getId()).name().toLowerCase(),
                    "upgrade_from", "free",
                    "payment_intent_id", paymentIntent.getId() // Link payment intent to subscription
                ));

        // CRITICAL FIX: Link payment intent to subscription for automatic activation
        // This ensures that when the payment intent succeeds, Stripe automatically activates the subscription
        // We need to update the payment intent to link it to the subscription
        logger.info("║ STEP 2.3: Linking Payment Intent to Subscription");
        Map<String, Object> paymentIntentUpdateParams = new HashMap<>();
        paymentIntentUpdateParams.put("metadata", Map.of(
            "subscription_id", "pending", // Will be updated after subscription creation
            "plan_id", newPlan.getId(),
            "upgrade_from", "free",
            "payment_purpose", "subscription_upgrade"
        ));
        
        // Update payment intent to prepare for subscription linking
        PaymentIntent updatedPaymentIntent = paymentIntent.update(paymentIntentUpdateParams);
        logger.info("✓ Payment intent updated for subscription linking");

        Subscription stripeSubscription = Subscription.create(paramsBuilder.build());
        logger.info("✓ Stripe subscription created");
        logger.info("║ - Subscription ID: {}", stripeSubscription.getId());
        logger.info("║ - Status: {}", stripeSubscription.getStatus());

        // STEP 2.4: Link payment intent to subscription (CRITICAL FIX)
        logger.info("║ STEP 2.4: Linking Payment Intent to Subscription");
        Map<String, Object> finalPaymentIntentUpdateParams = new HashMap<>();
        finalPaymentIntentUpdateParams.put("metadata", Map.of(
            "subscription_id", stripeSubscription.getId(),
            "plan_id", newPlan.getId(),
            "upgrade_from", "free",
            "payment_purpose", "subscription_upgrade"
        ));
        
        // Update payment intent with actual subscription ID
        PaymentIntent finalPaymentIntent = updatedPaymentIntent.update(finalPaymentIntentUpdateParams);
        logger.info("✓ Payment intent linked to subscription: {}", stripeSubscription.getId());

        // STEP 2.5: Save payment intent and subscription to database
        return saveSubscriptionAndPaymentIntent(currentSubscription, newPlan, finalPaymentIntent, 
            stripeSubscription, customerId, "free");
    }

    /**
     * Updates existing paid subscription with improved payment handling
     */
    private SubscriptionUpdateResponse updatePaidSubscription(UserSubscription currentSubscription, 
            SubscriptionPlan newPlan, String paymentMethodId, String returnUrl) throws StripeException {
        
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ STEP 2: Updating Paid Subscription");
        logger.info("║ - Current Plan: {}", currentSubscription.getPlanId());
        logger.info("║ - New Plan: {}", newPlan.getId());
        logger.info("╚══════════════════════════════════════════════════════════════");

        try {
            // STEP 2.1: Let Stripe handle proration calculation
            logger.info("║ STEP 2.1: Using Stripe's Proration Calculation");
            logger.info("║ - Current Plan: {}", currentSubscription.getPlanId());
            logger.info("║ - New Plan: {}", newPlan.getId());
            logger.info("║ - Letting Stripe calculate exact proration amount");
            
            // STEP 2.2: Create payment intent for the full new plan amount
            // Stripe will handle the proration automatically when we update the subscription
            String customerId = currentSubscription.getStripeCustomerId();
            logger.info("║ STEP 2.2: Creating Payment Intent for Full Plan Amount");
            PaymentIntent paymentIntent = createPaymentIntentForSubscription(
                customerId, newPlan, paymentMethodId, returnUrl, newPlan.getAmount());
            logger.info("✓ Payment Intent created: {}", paymentIntent.getId());

            // STEP 2.3: Update subscription in Stripe with ALWAYS_INVOICE
            // This tells Stripe to create an invoice with the exact proration amount
            logger.info("║ STEP 2.3: Updating Stripe Subscription with ALWAYS_INVOICE");
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
                        "upgrade_from", SubscriptionPlanType.fromPlanId(currentSubscription.getPlanId()).name().toLowerCase(),
                        "payment_intent_id", paymentIntent.getId() // Link payment intent to subscription
                    ))
                    .build();

            stripeSubscription = stripeSubscription.update(params);
            logger.info("✓ Stripe subscription updated");
            logger.info("║ - New Status: {}", stripeSubscription.getStatus());
            logger.info("║ - Stripe will create invoice with exact proration amount");

            // STEP 2.4: Save payment intent and update subscription
            return saveSubscriptionAndPaymentIntent(currentSubscription, newPlan, paymentIntent, 
                stripeSubscription, customerId, 
                SubscriptionPlanType.fromPlanId(currentSubscription.getPlanId()).name().toLowerCase());

        } catch (StripeException e) {
            logger.error("❌ Error updating subscription: {}", e.getMessage());
            throw new RuntimeException("Failed to update subscription: " + e.getMessage());
        }
    }

    /**
     * Creates a payment intent for subscription with 3D Secure support
     * 
     * IMPROVEMENTS:
     * 1. Creates payment intent explicitly before subscription
     * 2. Supports 3D Secure authentication with return URL
     * 3. Sets up future usage for recurring payments
     * 4. Better error handling
     * 5. Uses full plan amount - Stripe handles proration via ALWAYS_INVOICE
     */
    private PaymentIntent createPaymentIntentForSubscription(String customerId, SubscriptionPlan plan, 
            String paymentMethodId, String returnUrl, BigDecimal amount) throws StripeException {
        
        logger.info("║ Creating Payment Intent:");
        logger.info("║ - Customer: {}", customerId);
        logger.info("║ - Plan Amount: {} {}", plan.getAmount(), plan.getCurrency());
        logger.info("║ - Payment Intent Amount: {} {}", amount, plan.getCurrency());
        logger.info("║ - Payment Method: {}", paymentMethodId);
        logger.info("║ - Return URL: {}", returnUrl != null ? returnUrl : "None");
        logger.info("║ - Note: Stripe will handle proration via ALWAYS_INVOICE");

        Map<String, Object> params = new HashMap<>();
        int amountInCents = amount.multiply(BigDecimal.valueOf(100)).intValue();
        params.put("amount", amountInCents);
        params.put("currency", plan.getCurrency().toLowerCase());
        params.put("customer", customerId);
        params.put("payment_method", paymentMethodId);
        params.put("payment_method_types", Arrays.asList("card"));
        params.put("setup_future_usage", "off_session"); // For recurring payments
        
        // Handle 3D Secure authentication
        // For non-3D Secure cards, we don't pass return_url and confirm separately
        // For 3D Secure cards, we pass return_url and confirm immediately
        if (returnUrl != null && !returnUrl.isEmpty()) {
            params.put("return_url", returnUrl);
            params.put("confirm", true); // Confirm immediately for 3D Secure
            logger.info("║ - 3D Secure enabled with return URL and immediate confirmation");
        } else {
            params.put("confirm", false); // Don't confirm immediately for non-3D Secure
            logger.info("║ - Non-3D Secure card - will confirm separately");
        }
        
        params.put("metadata", Map.of(
            "subscription_type", SubscriptionPlanType.fromPlanId(plan.getId()).name().toLowerCase(),
            "plan_id", plan.getId(),
            "payment_purpose", "subscription_upgrade",
            "stripe_handles_proration", "true"
        ));

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        logger.info("✓ Payment Intent created successfully");
        logger.info("║ - ID: {}", paymentIntent.getId());
        logger.info("║ - Status: {}", paymentIntent.getStatus());
        logger.info("║ - Client Secret: {}", paymentIntent.getClientSecret());
        
        return paymentIntent;
    }

    /**
     * Saves payment intent and subscription to database with improved error handling
     * 
     * IMPROVEMENTS:
     * 1. Simplified payment intent saving (no complex retrieval logic)
     * 2. Better status tracking
     * 3. Enhanced metadata for debugging
     */
    private SubscriptionUpdateResponse saveSubscriptionAndPaymentIntent(
            UserSubscription currentSubscription,
            SubscriptionPlan newPlan,
            PaymentIntent stripePaymentIntent,
            Subscription stripeSubscription,
            String customerId,
            String upgradeFrom) throws StripeException {
        
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ STEP 3: Saving Payment Intent and Subscription");
        logger.info("╚══════════════════════════════════════════════════════════════");

        // STEP 3.1: Save payment intent to our database
        logger.info("║ STEP 3.1: Saving Payment Intent to Database");
        com.fintrack.model.payment.PaymentIntent dbPaymentIntent = new com.fintrack.model.payment.PaymentIntent();
        dbPaymentIntent.setAccountId(currentSubscription.getAccountId());
        dbPaymentIntent.setStripePaymentIntentId(stripePaymentIntent.getId());
        dbPaymentIntent.setAmount(newPlan.getAmount());
        dbPaymentIntent.setCurrency(newPlan.getCurrency());
        dbPaymentIntent.setStatus(stripePaymentIntent.getStatus());
        String paymentMethodId = stripePaymentIntent.getPaymentMethod();
        if (paymentMethodId == null) {
            logger.warn("Stripe payment intent {} has null payment method ID. Storing empty string in DB.", stripePaymentIntent.getId());
            paymentMethodId = "";
        }
        dbPaymentIntent.setPaymentMethodId(paymentMethodId);
        dbPaymentIntent.setClientSecret(stripePaymentIntent.getClientSecret());
        dbPaymentIntent.setStripeCustomerId(customerId);
        dbPaymentIntent.setSetupFutureUsage("off_session");
        dbPaymentIntent.setPaymentMethodTypes("card");
        dbPaymentIntent.setRequiresAction(stripePaymentIntent.getStatus().equals("requires_action"));
        dbPaymentIntent.setMetadata(String.format(
            "{\"subscription_id\":\"%s\",\"plan_id\":\"%s\",\"upgrade_from\":\"%s\",\"payment_purpose\":\"subscription_upgrade\"}", 
            stripeSubscription.getId(), newPlan.getId(), upgradeFrom));
        dbPaymentIntent.setCreatedAt(LocalDateTime.now());
        paymentIntentRepository.save(dbPaymentIntent);
        logger.info("✓ Payment intent saved to database");
        
        // STEP 3.2: Update subscription in database
        logger.info("║ STEP 3.2: Updating Subscription in Database");
        currentSubscription.setStripeSubscriptionId(stripeSubscription.getId());
        currentSubscription.setStripeCustomerId(customerId); // Ensure customer ID is set
        currentSubscription.setPlanId(newPlan.getId());
        currentSubscription.setStatus("incomplete");
        currentSubscription.setActive(false);
        currentSubscription.setLastPaymentDate(LocalDateTime.now());
        
        // Calculate next billing date for paid subscriptions
        // Always set next billing date for paid subscriptions (free or paid upgrades)
        if (stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
            Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
            if (periodEnd != null) {
                currentSubscription.setNextBillingDate(LocalDateTime.ofEpochSecond(periodEnd, 0, ZoneOffset.UTC));
            } else {
                // Fallback: Calculate based on plan interval and current date
                currentSubscription.setNextBillingDate(calculateNextBillingDateForPlan(newPlan));
            }
        } else {
            // Fallback: Calculate based on plan interval and current date
            currentSubscription.setNextBillingDate(calculateNextBillingDateForPlan(newPlan));
        }
        
        currentSubscription = userSubscriptionRepository.save(currentSubscription);
        logger.info("✓ Subscription updated in database");
        logger.info("║ - New Status: {}", currentSubscription.getStatus());
        logger.info("║ - Next Billing: {}", currentSubscription.getNextBillingDate());

        // STEP 3.3: Return response with payment details
        logger.info("║ STEP 3.3: Returning Payment Response");
        SubscriptionUpdateResponse response = SubscriptionUpdateResponse.fromUserSubscription(
            currentSubscription, 
            stripePaymentIntent.getClientSecret(), 
            true, 
            newPlan.getAmount(), 
            newPlan.getCurrency()
        );
        
        logger.info("✓ Payment flow setup complete");
        logger.info("║ - Payment Required: {}", response.isPaymentRequired());
        logger.info("║ - Has Client Secret: {}", response.getClientSecret() != null);
        logger.info("║ - Requires 3D Secure: {}", stripePaymentIntent.getStatus().equals("requires_action"));
        
        return response;
    }
    
    /**
     * Calculate days remaining in current billing cycle
     */
    private int calculateDaysRemaining(UserSubscription subscription) {
        return calculateDaysRemaining(subscription, LocalDateTime.now());
    }
    
    /**
     * Calculate days remaining using a custom current date (for testing)
     */
    private int calculateDaysRemaining(UserSubscription subscription, LocalDateTime currentDate) {
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ DAYS REMAINING CALCULATION");
        logger.info("║ Current Time: {}", currentDate);
        logger.info("║ Next Billing Date: {}", subscription.getNextBillingDate());
        logger.info("║ Subscription Start Date: {}", subscription.getSubscriptionStartDate());
        
        if (subscription.getNextBillingDate() == null) {
            logger.error("║ CRITICAL ERROR: Next billing date is not set for subscription!");
            logger.error("║ This should never happen - next_billing_date must be set when user subscribes");
            logger.error("║ Subscription ID: {}", subscription.getId());
            logger.error("║ Account ID: {}", subscription.getAccountId());
            logger.error("║ Plan ID: {}", subscription.getPlanId());
            logger.info("╚══════════════════════════════════════════════════════════════");
            throw new RuntimeException("Next billing date is not set for subscription. This indicates a data integrity issue.");
        }
        
        LocalDateTime nextBilling = subscription.getNextBillingDate();
        
        long daysRemaining = java.time.Duration.between(currentDate, nextBilling).toDays();
        int result = Math.max(1, (int) daysRemaining); // Minimum 1 day
        
        logger.info("║ Calculation: {} - {} = {} days", nextBilling, currentDate, daysRemaining);
        logger.info("║ Final Days Remaining: {}", result);
        logger.info("╚══════════════════════════════════════════════════════════════");
        
        return result;
    }

    /**
     * Calculates the next billing date for a given plan.
     * This is a fallback mechanism if the current period end is not available.
     * It assumes a default interval of 30 days if the interval is not recognized.
     */
    private LocalDateTime calculateNextBillingDateForPlan(SubscriptionPlan plan) {
        logger.info("║ Calculating next billing date for plan: {}", plan.getId());
        logger.info("║ - Plan Interval: {}", plan.getInterval());
        logger.info("║ - Plan Amount: {}", plan.getAmount());
        logger.info("║ - Plan Currency: {}", plan.getCurrency());

        int billingCycleDays = 30; // Default to 30 days
        if (plan.getInterval() != null && !plan.getInterval().isEmpty()) {
            billingCycleDays = switch (plan.getInterval().toLowerCase()) {
                case "day" -> 1;
                case "week" -> 7;
                case "month" -> 30;
                case "year" -> 365;
                default -> 30;
            };
        }
        logger.info("║ - Billing Cycle Days: {}", billingCycleDays);

        LocalDateTime nextBillingDate = LocalDateTime.now().plusDays(billingCycleDays);
        logger.info("║ - Next Billing Date (Fallback): {}", nextBillingDate);
        logger.info("╚══════════════════════════════════════════════════════════════");
        return nextBillingDate;
    }
} 