package com.fintrack.model.subscription;

import com.fintrack.model.user.Account;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription_change_audit")
@Data
@NoArgsConstructor
public class SubscriptionChangeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @Column(name = "change_type", nullable = false)
    private String changeType;

    @Column(name = "from_plan_id")
    private String fromPlanId;

    @Column(name = "to_plan_id")
    private String toPlanId;

    @Column(name = "proration_amount", precision = 10, scale = 2)
    private BigDecimal prorationAmount;

    @Column(name = "policy_version")
    private String policyVersion;

    @Column(name = "change_date", nullable = false)
    private LocalDateTime changeDate;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (changeDate == null) {
            changeDate = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionChangeAudit that = (SubscriptionChangeAudit) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
} 