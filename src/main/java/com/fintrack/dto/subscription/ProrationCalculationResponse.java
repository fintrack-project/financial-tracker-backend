package com.fintrack.dto.subscription;

import com.fintrack.model.subscription.ProrationCalculation;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProrationCalculationResponse {
    private Long id;
    private String calculationHash;
    private String fromPlanId;
    private String toPlanId;
    private Integer daysRemaining;
    private BigDecimal prorationAmount;
    private BigDecimal nextBillingAmount;
    private LocalDateTime calculationDate;
    private LocalDateTime expiresAt;

    public static ProrationCalculationResponse fromProrationCalculation(ProrationCalculation calculation) {
        ProrationCalculationResponse response = new ProrationCalculationResponse();
        response.setId(calculation.getId());
        response.setCalculationHash(calculation.getCalculationHash());
        response.setFromPlanId(calculation.getFromPlanId());
        response.setToPlanId(calculation.getToPlanId());
        response.setDaysRemaining(calculation.getDaysRemaining());
        response.setProrationAmount(calculation.getProrationAmount());
        response.setNextBillingAmount(calculation.getNextBillingAmount());
        response.setCalculationDate(calculation.getCalculationDate());
        response.setExpiresAt(calculation.getExpiresAt());
        return response;
    }
} 