package com.fintrack.model.subscription;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "proration_calculations")
@Data
@NoArgsConstructor
public class ProrationCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "calculation_hash", nullable = false, unique = true)
    private String calculationHash;

    @Column(name = "from_plan_id")
    private String fromPlanId;

    @Column(name = "to_plan_id")
    private String toPlanId;

    @Column(name = "days_remaining")
    private Integer daysRemaining;

    @Column(name = "proration_amount", precision = 10, scale = 2)
    private BigDecimal prorationAmount;

    @Column(name = "next_billing_amount", precision = 10, scale = 2)
    private BigDecimal nextBillingAmount;

    @Column(name = "calculation_date", nullable = false)
    private LocalDateTime calculationDate;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        calculationDate = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProrationCalculation that = (ProrationCalculation) o;
        return Objects.equals(calculationHash, that.calculationHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(calculationHash);
    }
} 