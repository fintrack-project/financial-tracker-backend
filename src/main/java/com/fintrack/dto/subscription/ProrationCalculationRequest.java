package com.fintrack.dto.subscription;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProrationCalculationRequest {
    private String fromPlanId;
    private String toPlanId;
    private Integer daysRemaining;
    private BigDecimal prorationAmount;
    private BigDecimal nextBillingAmount;
} 