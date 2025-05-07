package com.fintrack.dto.subscription;

import com.fintrack.model.subscription.PlanFeature;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.model.subscription.UserSubscription;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    private String status;  // "active", "pending", "failed"
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

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public String getStripePriceId() {
        return stripePriceId;
    }

    public void setStripePriceId(String stripePriceId) {
        this.stripePriceId = stripePriceId;
    }

    public String getPlanGroupId() {
        return planGroupId;
    }

    public void setPlanGroupId(String planGroupId) {
        this.planGroupId = planGroupId;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

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