package com.fintrack.dto.subscription;

import com.fintrack.model.subscription.UserSubscription;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubscriptionUpdateResponse {
    private String subscriptionId;
    private String status;
    private LocalDateTime currentPeriodEnd;
    private String planId;
    private String clientSecret;  // Stripe payment intent client secret
    private boolean paymentRequired;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;
    private String paymentUrl;    // URL for completing payment if needed

    public static SubscriptionUpdateResponse fromUserSubscription(UserSubscription subscription, 
            String clientSecret, boolean paymentRequired, BigDecimal amount, String currency) {
        SubscriptionUpdateResponse response = new SubscriptionUpdateResponse();
        response.setSubscriptionId(subscription.getStripeSubscriptionId());
        response.setStatus(subscription.getStatus());
        response.setCurrentPeriodEnd(subscription.getNextBillingDate());
        response.setPlanId(subscription.getPlanId());
        response.setClientSecret(clientSecret);
        response.setPaymentRequired(paymentRequired);
        response.setAmount(amount);
        response.setCurrency(currency);
        response.setPaymentStatus(subscription.getStatus());
        return response;
    }
} 