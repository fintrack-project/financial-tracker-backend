package com.fintrack.model.subscription;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription_plans")
@Data
@NoArgsConstructor
public class SubscriptionPlan {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Transient // Not in the database
    private String displayName;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";

    @Column(name = "interval")
    private String interval;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "plan_group_id")
    private String planGroupId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionPlan that = (SubscriptionPlan) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    // For backward compatibility with code that uses getPrice()
    public Double getPrice() {
        return amount != null ? amount.doubleValue() : null;
    }
    
    // For backward compatibility with code that uses getBillingCycle()
    public String getBillingCycle() {
        return interval;
    }
} 