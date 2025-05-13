package com.fintrack.service.subscription;

import com.fintrack.constants.subscription.SubscriptionPlanType;
import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionUpdateParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class UserSubscriptionDowngradeService extends BaseUserSubscriptionService {
    
    public UserSubscriptionDowngradeService(
            UserSubscriptionRepository userSubscriptionRepository,
            PaymentService paymentService,
            SubscriptionPlanService subscriptionPlanService,
            PaymentIntentRepository paymentIntentRepository) {
        super(userSubscriptionRepository, paymentService, subscriptionPlanService, paymentIntentRepository);
    }

    @Transactional
    public SubscriptionUpdateResponse downgradeSubscription(UUID accountId, String planId) throws StripeException {
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ STEP 1: Subscription Downgrade Initiation");
        logger.trace("║ Account: {}", accountId);
        logger.trace("║ Plan ID: {}", planId);
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