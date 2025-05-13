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
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionUpdateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class UserSubscriptionReactivateService extends BaseUserSubscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionReactivateService.class);
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public UserSubscriptionReactivateService(
            UserSubscriptionRepository userSubscriptionRepository,
            PaymentService paymentService,
            SubscriptionPlanService subscriptionPlanService,
            PaymentIntentRepository paymentIntentRepository) {
        super(userSubscriptionRepository, paymentService, subscriptionPlanService, paymentIntentRepository);
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
                    .setMetadata(Map.of(
                        "plan_id", subscription.getPlanId(),
                        "subscription_type", SubscriptionPlanType.fromPlanId(subscription.getPlanId()).name().toLowerCase(),
                        "reactivated_at", LocalDateTime.now().toString()
                    ))
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
            
            subscription.setNextBillingDate(nextBillingDate);
            
            logger.trace("╔══════════════════════════════════════════════════════════════");
            logger.trace("║ STEP 7: Updating Local Database");
            logger.trace("║ - Setting cancel_at_period_end to false");
            logger.trace("║ - Clearing subscription end date");
            logger.trace("║ - Next billing date: {}", nextBillingDate);
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
} 