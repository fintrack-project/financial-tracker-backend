package com.fintrack.service.subscription;

import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.Subscription;
import com.stripe.model.PaymentIntent;
import com.stripe.param.SubscriptionUpdateParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    public SubscriptionUpdateResponse cancelSubscription(String subscriptionId) throws StripeException {
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 1: Subscription Cancellation Initiation");
        logger.trace("║ Subscription ID: {}", subscriptionId);
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
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
} 