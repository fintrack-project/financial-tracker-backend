package com.fintrack.service.subscription;

import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Service
public class UserSubscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionService.class);
    
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserSubscriptionUpgradeService upgradeService;
    private final UserSubscriptionCancellationService cancellationService;
    private final UserSubscriptionReactivateService reactivateService;
    private final UserSubscriptionDowngradeService downgradeService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final PaymentService paymentService;
    private final PaymentIntentRepository paymentIntentRepository;

    public UserSubscriptionService(
            UserSubscriptionRepository userSubscriptionRepository,
            UserSubscriptionUpgradeService upgradeService,
            UserSubscriptionCancellationService cancellationService,
            UserSubscriptionReactivateService reactivateService,
            UserSubscriptionDowngradeService downgradeService,
            SubscriptionPlanService subscriptionPlanService,
            PaymentService paymentService,
            PaymentIntentRepository paymentIntentRepository) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.upgradeService = upgradeService;
        this.cancellationService = cancellationService;
        this.reactivateService = reactivateService;
        this.downgradeService = downgradeService;
        this.subscriptionPlanService = subscriptionPlanService;
        this.paymentService = paymentService;
        this.paymentIntentRepository = paymentIntentRepository;
    }

    public Optional<UserSubscription> getSubscriptionByAccountId(UUID accountId) {
        return userSubscriptionRepository.findByAccountId(accountId);
    }

    @Transactional
    public SubscriptionUpdateResponse upgradeSubscriptionWithPayment(
            UUID accountId, 
            String planId, 
            String paymentMethodId,
            String returnUrl) throws StripeException {
        
        logger.info("Upgrading subscription for account: {} to plan: {}", accountId, planId);
        
        // Validate the plan exists
        Optional<SubscriptionPlan> planOpt = subscriptionPlanService.getPlanById(planId);
        if (planOpt.isEmpty()) {
            throw new RuntimeException("Invalid plan ID: " + planId);
        }
        
        // Get current subscription
        UserSubscription currentSubscription = userSubscriptionRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("No subscription found for account: " + accountId));
        
        // Perform the upgrade
        return upgradeService.upgradeSubscription(accountId, planId, paymentMethodId, returnUrl);
    }

    @Transactional
    public SubscriptionUpdateResponse confirmPayment(String paymentIntentId, String stripeSubscriptionId) throws StripeException {
        logger.info("Confirming payment for subscription: {} with payment intent: {}", stripeSubscriptionId, paymentIntentId);
        
        // Get the subscription
        UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + stripeSubscriptionId));
        
        // Get the payment intent
        com.fintrack.model.payment.PaymentIntent paymentIntent = paymentIntentRepository
                .findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment intent not found: " + paymentIntentId));
        
        // Log payment intent details before confirmation
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ Payment Intent Details (Before Confirmation)");
        logger.trace("║ - ID: {}", paymentIntent.getStripePaymentIntentId());
        logger.trace("║ - Status: {}", paymentIntent.getStatus());
        logger.trace("║ - Amount: {}", paymentIntent.getAmount());
        logger.trace("║ - Currency: {}", paymentIntent.getCurrency());
        logger.trace("║ - Payment Method ID: {}", paymentIntent.getPaymentMethodId());
        logger.trace("║ - Client Secret: {}", paymentIntent.getClientSecret());
        logger.trace("║ - Requires Action: {}", paymentIntent.getStatus().equals("requires_action"));
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
        // Get updated payment intent from Stripe
        com.stripe.model.PaymentIntent stripePaymentIntent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ Payment Intent Details (After Frontend Confirmation)");
        logger.trace("║ - ID: {}", stripePaymentIntent.getId());
        logger.trace("║ - Status: {}", stripePaymentIntent.getStatus());
        logger.trace("║ - Amount: {}", stripePaymentIntent.getAmount());
        logger.trace("║ - Currency: {}", stripePaymentIntent.getCurrency());
        logger.trace("║ - Payment Method Types: {}", stripePaymentIntent.getPaymentMethodTypes());
        logger.trace("║ - Client Secret: {}", stripePaymentIntent.getClientSecret());
        logger.trace("║ - Requires Action: {}", stripePaymentIntent.getStatus().equals("requires_action"));
        logger.trace("║ - Requires Confirmation: {}", stripePaymentIntent.getStatus().equals("requires_confirmation"));
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
        // Update payment intent status in database
        paymentIntent.setStatus(stripePaymentIntent.getStatus());
        paymentIntentRepository.save(paymentIntent);
        
        // Update subscription status based on payment intent status
        String paymentStatus = stripePaymentIntent.getStatus();
        logger.info("Payment status: {}, Current subscription status: {}", paymentStatus, subscription.getStatus());
        
        if ("succeeded".equals(paymentStatus) || "processing".equals(paymentStatus)) {
            // Update local database only - Stripe webhook will handle subscription status
            subscription.setLastPaymentDate(java.time.LocalDateTime.now());
            subscription = userSubscriptionRepository.save(subscription);
            logger.info("Updated last payment date for subscription: {}", stripeSubscriptionId);
        }
        
        return SubscriptionUpdateResponse.fromUserSubscription(subscription, null, false, null, null);
    }

    @Transactional
    public SubscriptionUpdateResponse cancelSubscription(String stripeSubscriptionId) throws StripeException {
        logger.info("Canceling subscription: {}", stripeSubscriptionId);
        
        // Perform the cancellation
        return cancellationService.cancelSubscription(stripeSubscriptionId);
    }

    @Transactional
    public SubscriptionUpdateResponse reactivateSubscription(String stripeSubscriptionId) throws StripeException {
        logger.info("Reactivating subscription: {}", stripeSubscriptionId);
        
        // Delegate to the reactivate service
        return reactivateService.reactivateSubscription(stripeSubscriptionId);
    }

    @Transactional
    public SubscriptionUpdateResponse downgradeSubscriptionWithPayment(
            UUID accountId, 
            String planId) throws StripeException {
        
        logger.info("Downgrading subscription for account: {} to plan: {}", accountId, planId);
        
        // Validate the plan exists
        Optional<SubscriptionPlan> planOpt = subscriptionPlanService.getPlanById(planId);
        if (planOpt.isEmpty()) {
            throw new RuntimeException("Invalid plan ID: " + planId);
        }
        
        // Get current subscription
        UserSubscription currentSubscription = userSubscriptionRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("No subscription found for account: " + accountId));
        
        // Perform the downgrade
        return downgradeService.downgradeSubscription(accountId, planId);
    }

    @Transactional
    public void createFreeSubscription(UUID accountId, String planName) {
        // Find the free plan by name
        Optional<SubscriptionPlan> planOpt = subscriptionPlanService.getPlanById("plan_free");
        if (planOpt.isEmpty()) {
            throw new RuntimeException("Free plan not found");
        }
        SubscriptionPlan plan = planOpt.get();

        // Create a new UserSubscription
        UserSubscription subscription = new UserSubscription();
        subscription.setAccountId(accountId);
        subscription.setPlanId(plan.getId());
        subscription.setStripeSubscriptionId("free_" + accountId.toString());
        subscription.setStatus("active");
        subscription.setActive(true);
        subscription.setSubscriptionStartDate(java.time.LocalDateTime.now());
        subscription.setNextBillingDate(null);
        subscription.setCreatedAt(java.time.LocalDateTime.now());
        subscription.setCancelAtPeriodEnd(false);
        userSubscriptionRepository.save(subscription);
        logger.info("Created free subscription for account: {}", accountId);
    }

    // Stubbed webhook handler methods for StripeWebhookController
    public void handlePaymentRequiresAction(String paymentIntentId, String subscriptionId, String nextAction) {
        logger.info("[Webhook] handlePaymentRequiresAction called: paymentIntentId={}, subscriptionId={}, nextAction={}", paymentIntentId, subscriptionId, nextAction);
    }

    public void handleSubscriptionCreated(String subscriptionId, String customerId) {
        logger.info("[Webhook] handleSubscriptionCreated called: subscriptionId={}, customerId={}", subscriptionId, customerId);
    }

    public void handleSubscriptionUpdated(String subscriptionId, String status, Boolean cancelAtPeriodEnd) {
        logger.info("[Webhook] handleSubscriptionUpdated called: subscriptionId={}, status={}, cancelAtPeriodEnd={}", 
            subscriptionId, status, cancelAtPeriodEnd);
        
        // Get the subscription from our database
        UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        // Update subscription status
        subscription.setStatus(status);
        subscription.setActive("active".equals(status));
        subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
        
        // If subscription is active, update last payment date
        if ("active".equals(status)) {
            subscription.setLastPaymentDate(java.time.LocalDateTime.now());
        }
        
        // Save the updated subscription
        subscription = userSubscriptionRepository.save(subscription);
        logger.info("Updated subscription status to {} for subscription: {}", status, subscriptionId);
    }

    public void handleSubscriptionDeleted(String subscriptionId) {
        logger.info("[Webhook] handleSubscriptionDeleted called: subscriptionId={}", subscriptionId);
    }
}
