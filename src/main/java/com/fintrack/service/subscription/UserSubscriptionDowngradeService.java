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
        Map<String, Object> details = new HashMap<>();
        details.put("accountId", accountId);
        details.put("planId", planId);
        logOperation("Downgrade Subscription", details);

        // Get current subscription
        UserSubscription currentSubscription = userSubscriptionRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("No subscription found for account: " + accountId));
        validateSubscriptionExists(currentSubscription, currentSubscription.getStripeSubscriptionId());

        // Get plan details
        SubscriptionPlan newPlan = subscriptionPlanService.getPlanById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));
        validatePlanExists(newPlan, planId);

        // Validate downgrade
        validateDowngrade(currentSubscription, newPlan);

        // Update subscription in Stripe
        Subscription stripeSubscription = updateStripeSubscription(
                currentSubscription.getStripeSubscriptionId(), 
                newPlan.getStripePriceId());

        // Update subscription in database
        currentSubscription.setPlanId(newPlan.getId());
        currentSubscription.setStatus(stripeSubscription.getStatus());
        currentSubscription.setActive("active".equals(stripeSubscription.getStatus()));
        currentSubscription.setNextBillingDate(calculateNextBillingDate(newPlan));
        currentSubscription = userSubscriptionRepository.save(currentSubscription);

        return SubscriptionUpdateResponse.fromUserSubscription(currentSubscription, 
                null, false, newPlan.getAmount(), newPlan.getCurrency());
    }

    private void validateDowngrade(UserSubscription currentSubscription, SubscriptionPlan newPlan) {
        SubscriptionPlan currentPlan = subscriptionPlanService.getPlanById(currentSubscription.getPlanId())
                .orElseThrow(() -> new RuntimeException("Current plan not found: " + currentSubscription.getPlanId()));

        if (newPlan.getAmount().compareTo(currentPlan.getAmount()) >= 0) {
            throw new RuntimeException("New plan must have a lower price than current plan");
        }

        // Additional validation for downgrade-specific rules
        if (currentSubscription.getStripeSubscriptionId().startsWith("free_")) {
            throw new RuntimeException("Cannot downgrade a free subscription");
        }
    }

    private Subscription updateStripeSubscription(String subscriptionId, String newPriceId) throws StripeException {
        try {
            Subscription stripeSubscription = Subscription.retrieve(subscriptionId);
            String subscriptionItemId = stripeSubscription.getItems().getData().get(0).getId();

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(subscriptionItemId)
                            .setPrice(newPriceId)
                            .build())
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.CREATE_PRORATIONS)
                    .build();

            return stripeSubscription.update(params);
        } catch (StripeException e) {
            handleStripeError(e, "update Stripe subscription");
            throw e;
        }
    }
} 