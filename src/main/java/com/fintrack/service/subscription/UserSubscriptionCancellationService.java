package com.fintrack.service.subscription;

import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.Subscription;
import com.stripe.model.PaymentIntent;
import com.stripe.param.SubscriptionCancelParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserSubscriptionCancellationService extends BaseUserSubscriptionService {
    
    public UserSubscriptionCancellationService(
            UserSubscriptionRepository userSubscriptionRepository,
            PaymentService paymentService,
            SubscriptionPlanService subscriptionPlanService,
            PaymentIntentRepository paymentIntentRepository) {
        super(userSubscriptionRepository, paymentService, subscriptionPlanService, paymentIntentRepository);
    }

    @Transactional
    public SubscriptionUpdateResponse cancelSubscription(UUID accountId, boolean immediate) throws StripeException {
        Map<String, Object> details = new HashMap<>();
        details.put("accountId", accountId);
        details.put("immediate", immediate);
        logOperation("Cancel Subscription", details);

        // Get current subscription
        UserSubscription currentSubscription = userSubscriptionRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("No subscription found for account: " + accountId));
        validateSubscriptionExists(currentSubscription, currentSubscription.getStripeSubscriptionId());

        // Validate cancellation
        validateCancellation(currentSubscription);

        // Handle cancellation based on type
        if (immediate) {
            return handleImmediateCancellation(currentSubscription);
        } else {
            return handleEndOfPeriodCancellation(currentSubscription);
        }
    }

    private void validateCancellation(UserSubscription subscription) {
        if (subscription.getStripeSubscriptionId().startsWith("free_")) {
            throw new RuntimeException("Cannot cancel a free subscription");
        }

        if (!subscription.isActive()) {
            throw new RuntimeException("Subscription is already inactive");
        }
    }

    private SubscriptionUpdateResponse handleImmediateCancellation(UserSubscription subscription) throws StripeException {
        try {
            // Cancel subscription in Stripe
            Subscription stripeSubscription = cancelStripeSubscription(subscription.getStripeSubscriptionId(), true);

            // Process refund if needed
            processRefundIfNeeded(subscription);

            // Update subscription in database
            subscription.setStatus("canceled");
            subscription.setActive(false);
            subscription = userSubscriptionRepository.save(subscription);

            return SubscriptionUpdateResponse.fromUserSubscription(subscription, null, false, null, null);

        } catch (StripeException e) {
            handleStripeError(e, "immediate cancellation");
            throw e;
        }
    }

    private SubscriptionUpdateResponse handleEndOfPeriodCancellation(UserSubscription subscription) throws StripeException {
        try {
            // Cancel subscription at period end in Stripe
            Subscription stripeSubscription = cancelStripeSubscription(subscription.getStripeSubscriptionId(), false);

            // Update subscription in database
            subscription.setStatus("canceled_at_period_end");
            subscription = userSubscriptionRepository.save(subscription);

            return SubscriptionUpdateResponse.fromUserSubscription(subscription, null, false, null, null);

        } catch (StripeException e) {
            handleStripeError(e, "end of period cancellation");
            throw e;
        }
    }

    private Subscription cancelStripeSubscription(String subscriptionId, boolean immediate) throws StripeException {
        try {
            Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
            
            SubscriptionCancelParams params = SubscriptionCancelParams.builder()
                    .setProrate(immediate)
                    .build();

            return stripeSubscription.cancel(params);
        } catch (StripeException e) {
            handleStripeError(e, "cancel Stripe subscription");
            throw e;
        }
    }

    private void processRefundIfNeeded(UserSubscription subscription) throws StripeException {
        try {
            // Get the latest payment for this subscription
            Optional<com.fintrack.model.payment.PaymentIntent> latestPayment = paymentIntentRepository
                    .findByStripeSubscriptionIdOrderByCreatedAtDesc(subscription.getStripeSubscriptionId())
                    .stream()
                    .findFirst();

            if (latestPayment.isPresent()) {
                com.fintrack.model.payment.PaymentIntent payment = latestPayment.get();
                
                // Calculate refund amount based on unused time
                long refundAmount = calculateRefundAmount(payment.getAmount().longValue(), subscription);
                
                if (refundAmount > 0) {
                    Map<String, Object> refundParams = new HashMap<>();
                    refundParams.put("payment_intent", payment.getStripePaymentIntentId());
                    refundParams.put("amount", refundAmount);
                    refundParams.put("reason", "requested_by_customer");
                    
                    Refund.create(refundParams);
                    logger.info("Processed refund of {} for subscription {}", refundAmount, subscription.getId());
                }
            }
        } catch (StripeException e) {
            handleStripeError(e, "process refund");
            throw e;
        }
    }

    private long calculateRefundAmount(long totalAmount, UserSubscription subscription) {
        if (subscription.getNextBillingDate() == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextBilling = subscription.getNextBillingDate();
        
        if (now.isAfter(nextBilling)) {
            return 0;
        }

        // Calculate the proportion of unused time
        long totalSeconds = subscription.getCreatedAt().until(nextBilling, java.time.temporal.ChronoUnit.SECONDS);
        long remainingSeconds = now.until(nextBilling, java.time.temporal.ChronoUnit.SECONDS);
        
        if (totalSeconds <= 0) {
            return 0;
        }

        // Calculate refund amount proportionally
        return (long) ((double) remainingSeconds / totalSeconds * totalAmount);
    }
} 