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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class NewUserSubscriptionService extends BaseUserSubscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(NewUserSubscriptionService.class);
    
    public NewUserSubscriptionService(
            UserSubscriptionRepository userSubscriptionRepository,
            PaymentService paymentService,
            SubscriptionPlanService subscriptionPlanService,
            PaymentIntentRepository paymentIntentRepository) {
        super(userSubscriptionRepository, paymentService, subscriptionPlanService, paymentIntentRepository);
    }

    /**
     * Creates a new subscription for a user with improved payment handling
     * 
     * CHANGES FROM PREVIOUS VERSION:
     * 1. Uses ALLOW_INCOMPLETE instead of DEFAULT_INCOMPLETE to avoid race conditions
     * 2. Creates payment intent explicitly before creating subscription
     * 3. Supports 3D Secure authentication with return URL
     * 4. Simplified payment intent handling (no complex retrieval logic)
     * 5. Enhanced logging for better debugging
     * 
     * @param accountId User's account ID
     * @param planId Plan ID to subscribe to
     * @param paymentMethodId Stripe payment method ID
     * @param returnUrl URL to return to after 3D Secure authentication (for 3D Secure cards)
     * @return SubscriptionUpdateResponse with payment details
     */
    @Transactional
    public SubscriptionUpdateResponse createSubscription(UUID accountId, String planId, 
            String paymentMethodId, String returnUrl) throws StripeException {
        
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ STEP 1: New Subscription Creation");
        logger.info("║ Account: {}", accountId);
        logger.info("║ Plan ID: {}", planId);
        logger.info("║ Payment Method: {}", paymentMethodId);
        logger.info("║ Return URL: {}", returnUrl != null ? returnUrl : "None (3D Secure not required)");
        logger.info("╚══════════════════════════════════════════════════════════════");

        Map<String, Object> details = new HashMap<>();
        details.put("accountId", accountId);
        details.put("planId", planId);
        details.put("paymentMethodId", paymentMethodId);
        logOperation("Create New Subscription", details);

        // Get plan details
        SubscriptionPlan plan = subscriptionPlanService.getPlanById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));
        validatePlanExists(plan, planId);

        // Ensure customer exists in Stripe
        String customerId = ensureStripeCustomerExists(accountId);
        
        // STEP 2.1: Create payment intent first (NEW APPROACH)
        logger.info("║ STEP 2.1: Creating Payment Intent");
        PaymentIntent paymentIntent = createPaymentIntentForSubscription(
            customerId, plan, paymentMethodId, returnUrl);
        logger.info("✓ Payment Intent created: {}", paymentIntent.getId());
        logger.info("║ - Status: {}", paymentIntent.getStatus());
        logger.info("║ - Requires Action: {}", paymentIntent.getStatus().equals("requires_action"));
        
        // STEP 2.2: Create subscription with ALLOW_INCOMPLETE and attach payment intent
        logger.info("║ STEP 2.2: Creating Subscription with ALLOW_INCOMPLETE");
        Subscription stripeSubscription = createStripeSubscription(customerId, plan.getStripePriceId(), 
            paymentMethodId, paymentIntent.getId());
        
        // STEP 2.3: Link payment intent to subscription (CRITICAL FIX)
        logger.info("║ STEP 2.3: Linking Payment Intent to Subscription");
        Map<String, Object> paymentIntentUpdateParams = new HashMap<>();
        paymentIntentUpdateParams.put("metadata", Map.of(
            "subscription_id", stripeSubscription.getId(),
            "plan_id", planId,
            "payment_purpose", "new_subscription"
        ));
        
        // Update payment intent with actual subscription ID
        PaymentIntent updatedPaymentIntent = paymentIntent.update(paymentIntentUpdateParams);
        logger.info("✓ Payment intent linked to subscription: {}", stripeSubscription.getId());
        
        // STEP 2.4: Save subscription and payment intent to database
        logger.info("║ STEP 2.4: Saving Subscription and Payment Intent");
        UserSubscription subscription = saveSubscription(accountId, planId, customerId, stripeSubscription, updatedPaymentIntent);
        
        logger.info("✓ New subscription created successfully");
        logger.info("║ - Subscription ID: {}", subscription.getId());
        logger.info("║ - Stripe Subscription ID: {}", subscription.getStripeSubscriptionId());
        logger.info("║ - Payment Required: {}", updatedPaymentIntent.getStatus().equals("requires_confirmation"));
        logger.info("║ - Requires 3D Secure: {}", updatedPaymentIntent.getStatus().equals("requires_action"));
        
        return SubscriptionUpdateResponse.fromUserSubscription(subscription, 
                updatedPaymentIntent.getClientSecret(), true, plan.getAmount(), plan.getCurrency());
    }

    /**
     * Creates a Stripe subscription with ALLOW_INCOMPLETE payment behavior
     * 
     * IMPROVEMENTS:
     * 1. Uses ALLOW_INCOMPLETE instead of DEFAULT_INCOMPLETE
     * 2. Links payment intent to subscription via metadata
     * 3. Enables 3D Secure authentication
     * 4. Better error handling
     */
    private Subscription createStripeSubscription(String customerId, String stripePriceId, 
            String paymentMethodId, String paymentIntentId) throws StripeException {
        try {
            SubscriptionCreateParams.Builder paramsBuilder = SubscriptionCreateParams.builder()
                    .setCustomer(customerId)
                    .addItem(SubscriptionCreateParams.Item.builder()
                            .setPrice(stripePriceId)
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
                            .build());

            if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
                paramsBuilder.setDefaultPaymentMethod(paymentMethodId);
            }

            // Link payment intent to subscription via metadata
            paramsBuilder.setMetadata(Map.of(
                "payment_intent_id", paymentIntentId,
                "subscription_type", "new_subscription"
            ));

            return Subscription.create(paramsBuilder.build());
        } catch (StripeException e) {
            handleStripeError(e, "create Stripe subscription");
            throw e;
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
     */
    private PaymentIntent createPaymentIntentForSubscription(String customerId, SubscriptionPlan plan, 
            String paymentMethodId, String returnUrl) throws StripeException {
        
        logger.info("║ Creating Payment Intent:");
        logger.info("║ - Customer: {}", customerId);
        logger.info("║ - Amount: {} {}", plan.getAmount(), plan.getCurrency());
        logger.info("║ - Payment Method: {}", paymentMethodId);
        logger.info("║ - Return URL: {}", returnUrl != null ? returnUrl : "None");

        Map<String, Object> params = new HashMap<>();
        int amountInCents = plan.getAmount().multiply(BigDecimal.valueOf(100)).intValue();
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
        
        // Note: We can't set subscription_id here because the subscription doesn't exist yet
        // The subscription_id will be set in the database metadata after subscription creation
        params.put("metadata", Map.of(
            "subscription_type", SubscriptionPlanType.fromPlanId(plan.getId()).name().toLowerCase(),
            "plan_id", plan.getId(),
            "payment_purpose", "new_subscription"
        ));

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        logger.info("✓ Payment Intent created successfully");
        logger.info("║ - ID: {}", paymentIntent.getId());
        logger.info("║ - Status: {}", paymentIntent.getStatus());
        logger.info("║ - Client Secret: {}", paymentIntent.getClientSecret());
        
        return paymentIntent;
    }

    /**
     * Saves subscription and payment intent to database with improved error handling
     * 
     * IMPROVEMENTS:
     * 1. Saves both subscription and payment intent
     * 2. Better status tracking
     * 3. Enhanced metadata for debugging
     */
    private UserSubscription saveSubscription(UUID accountId, String planId, String customerId, 
            Subscription stripeSubscription, PaymentIntent paymentIntent) throws StripeException {
        
        logger.info("║ Saving Subscription and Payment Intent to Database");
        
        // Save payment intent to database
        com.fintrack.model.payment.PaymentIntent dbPaymentIntent = new com.fintrack.model.payment.PaymentIntent();
        dbPaymentIntent.setAccountId(accountId);
        dbPaymentIntent.setStripePaymentIntentId(paymentIntent.getId());
        dbPaymentIntent.setAmount(BigDecimal.valueOf(paymentIntent.getAmount()).divide(BigDecimal.valueOf(100)));
        dbPaymentIntent.setCurrency(paymentIntent.getCurrency().toUpperCase());
        dbPaymentIntent.setStatus(paymentIntent.getStatus());
        dbPaymentIntent.setPaymentMethodId(paymentIntent.getPaymentMethod());
        dbPaymentIntent.setClientSecret(paymentIntent.getClientSecret());
        dbPaymentIntent.setStripeCustomerId(customerId);
        dbPaymentIntent.setSetupFutureUsage("off_session");
        dbPaymentIntent.setPaymentMethodTypes("card");
        dbPaymentIntent.setRequiresAction(paymentIntent.getStatus().equals("requires_action"));
        dbPaymentIntent.setMetadata(String.format(
            "{\"subscription_id\":\"%s\",\"plan_id\":\"%s\",\"payment_purpose\":\"new_subscription\"}", 
            stripeSubscription.getId(), planId));
        dbPaymentIntent.setCreatedAt(LocalDateTime.now());
        paymentIntentRepository.save(dbPaymentIntent);
        logger.info("✓ Payment intent saved to database");
        
        // Save subscription to database
        UserSubscription subscription = new UserSubscription();
        subscription.setAccountId(accountId);
        subscription.setPlanId(planId);
        subscription.setStripeSubscriptionId(stripeSubscription.getId());
        subscription.setStripeCustomerId(customerId);
        subscription.setStatus("incomplete");
        subscription.setActive(false);
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setLastPaymentDate(LocalDateTime.now());
        
        // Calculate next billing date
        if (stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
            Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
            if (periodEnd != null) {
                subscription.setNextBillingDate(LocalDateTime.ofEpochSecond(periodEnd, 0, ZoneOffset.UTC));
            } else {
                subscription.setNextBillingDate(LocalDateTime.now().plusDays(30));
            }
        } else {
            subscription.setNextBillingDate(LocalDateTime.now().plusDays(30));
        }
        
        subscription = userSubscriptionRepository.save(subscription);
        logger.info("✓ Subscription saved to database");
        logger.info("║ - Status: {}", subscription.getStatus());
        logger.info("║ - Next Billing: {}", subscription.getNextBillingDate());
        
        return subscription;
    }
} 