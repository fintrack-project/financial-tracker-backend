package com.fintrack.dto.subscription;

import com.fintrack.model.subscription.PlanFeature;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.model.subscription.UserSubscription;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubscriptionPlanResponse {
    // Plan details
    private String id;
    private String name;
    private String displayName;
    private BigDecimal amount;
    private String currency;
    private String interval;
    private String stripePriceId;
    private String planGroupId;
    private List<String> features;
    private LocalDateTime createdAt;

    // Subscription details
    private String subscriptionId;
    private String status;  // Stripe standard statuses: "active", "canceled", "incomplete", "incomplete_expired", "past_due", "trialing", "unpaid", "paused"
    private LocalDateTime currentPeriodEnd;
    private String clientSecret;  // Stripe payment intent client secret
    private boolean paymentRequired;
    private String paymentStatus;
    private String paymentUrl;

    public static SubscriptionPlanResponse fromSubscriptionPlan(SubscriptionPlan plan, List<PlanFeature> features) {
        SubscriptionPlanResponse response = new SubscriptionPlanResponse();
        response.setId(plan.getId());
        response.setName(plan.getName());
        response.setDisplayName(plan.getDisplayName());
        response.setAmount(plan.getAmount());
        response.setCurrency(plan.getCurrency());
        response.setInterval(plan.getInterval());
        response.setStripePriceId(plan.getStripePriceId());
        response.setPlanGroupId(plan.getPlanGroupId());
        response.setCreatedAt(plan.getCreatedAt());
        
        if (features != null) {
            response.setFeatures(features.stream()
                .map(PlanFeature::getFeatureName)
                .collect(Collectors.toList()));
        }
        
        return response;
    }

    public static SubscriptionPlanResponse fromUserSubscription(UserSubscription subscription, 
            String clientSecret, boolean paymentRequired, BigDecimal amount, String currency) {
        SubscriptionPlanResponse response = new SubscriptionPlanResponse();
        response.setSubscriptionId(subscription.getStripeSubscriptionId());
        response.setStatus(subscription.getStatus());
        response.setCurrentPeriodEnd(subscription.getNextBillingDate());
        response.setId(subscription.getPlanId());
        response.setClientSecret(clientSecret);
        response.setPaymentRequired(paymentRequired);
        response.setAmount(amount);
        response.setCurrency(currency);
        response.setPaymentStatus(subscription.getStatus());
        return response;
    }
} 