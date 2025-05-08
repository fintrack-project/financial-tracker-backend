package com.fintrack.dto.subscription;

import com.fintrack.model.subscription.UserSubscription;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    // Getters and Setters
    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isPaymentRequired() {
        return paymentRequired;
    }

    public void setPaymentRequired(boolean paymentRequired) {
        this.paymentRequired = paymentRequired;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }
} 