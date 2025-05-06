package com.fintrack.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "stripe_price_id", nullable = false)
    private String stripePriceId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, columnDefinition = "varchar(3) DEFAULT 'USD'")
    private String currency;

    @Column(name = "interval", nullable = false, length = 20)
    private String interval;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "plan_group_id")
    private String planGroupId;

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

    public String getStripePriceId() {
        return stripePriceId;
    }

    public void setStripePriceId(String stripePriceId) {
        this.stripePriceId = stripePriceId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPlanGroupId() {
        return planGroupId;
    }

    public void setPlanGroupId(String planGroupId) {
        this.planGroupId = planGroupId;
    }
} 