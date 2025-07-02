package com.fintrack.dto.subscription;

import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.model.subscription.UserSubscription;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO that combines user subscription details with the associated plan details
 */
@Data
@NoArgsConstructor
public class UserSubscriptionDetailsResponse {
    private UserSubscription subscription;
    private SubscriptionPlan plan;
} 