package com.fintrack.dto.subscription;

import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.model.subscription.UserSubscription;

/**
 * DTO that combines user subscription details with the associated plan details
 */
public class UserSubscriptionDetailsResponse {
    private UserSubscription subscription;
    private SubscriptionPlan plan;

    // Getters and Setters
    public UserSubscription getSubscription() {
        return subscription;
    }

    public void setSubscription(UserSubscription subscription) {
        this.subscription = subscription;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }
} 