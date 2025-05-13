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
    public SubscriptionUpdateResponse confirmPayment(String paymentIntentId, String subscriptionId) throws StripeException {
        logger.info("Confirming payment for subscription: {} with payment intent: {}", subscriptionId, paymentIntentId);
        
        // Get the subscription
        UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        // Get the payment intent
        com.fintrack.model.payment.PaymentIntent paymentIntent = paymentIntentRepository
                .findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment intent not found: " + paymentIntentId));
        
        // Confirm the payment in Stripe
        paymentService.confirmPaymentIntent(paymentIntentId, paymentIntent.getPaymentMethodId());
        
        // Update subscription status
        subscription.setStatus("active");
        subscription.setActive(true);
        subscription.setLastPaymentDate(java.time.LocalDateTime.now());
        subscription = userSubscriptionRepository.save(subscription);
        
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
}
