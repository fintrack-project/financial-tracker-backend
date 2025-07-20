package com.fintrack.controller.subscription;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.service.subscription.SubscriptionPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/subscription-policies", produces = MediaType.APPLICATION_JSON_VALUE)
public class ProrationController {

    private static final Logger logger = LoggerFactory.getLogger(ProrationController.class);
    private final SubscriptionPlanService subscriptionPlanService;

    public ProrationController(SubscriptionPlanService subscriptionPlanService) {
        this.subscriptionPlanService = subscriptionPlanService;
    }

    /**
     * GET /api/subscription-policies/calculations/proration - Calculate proration for display
     */
    @GetMapping("/calculations/proration")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateProration(
            @RequestParam String fromPlanId,
            @RequestParam String toPlanId,
            @RequestParam Integer daysRemaining) {
        try {
            logger.info("╔══════════════════════════════════════════════════════════════");
            logger.info("║ PRORATION CALCULATION REQUEST (Display Only)");
            logger.info("║ From Plan ID: {}", fromPlanId);
            logger.info("║ To Plan ID: {}", toPlanId);
            logger.info("║ Days Remaining: {}", daysRemaining);
            
            // Get plan details
            Optional<SubscriptionPlan> fromPlan = subscriptionPlanService.getPlanById(fromPlanId);
            Optional<SubscriptionPlan> toPlan = subscriptionPlanService.getPlanById(toPlanId);
            
            if (fromPlan.isEmpty() || toPlan.isEmpty()) {
                logger.error("║ Invalid plan ID provided - From: {}, To: {}", fromPlanId, toPlanId);
                return ResponseWrapper.badRequest("Invalid plan ID provided");
            }
            
            SubscriptionPlan currentPlan = fromPlan.get();
            SubscriptionPlan newPlan = toPlan.get();
            
            logger.info("║ Current Plan: {} - ${} ({})", currentPlan.getId(), currentPlan.getAmount(), currentPlan.getInterval());
            logger.info("║ New Plan: {} - ${} ({})", newPlan.getId(), newPlan.getAmount(), newPlan.getInterval());
            
            // Calculate proration for display purposes
            BigDecimal prorationAmount = calculateProrationAmount(
                currentPlan.getAmount(), 
                newPlan.getAmount(), 
                daysRemaining, 
                currentPlan.getInterval(), 
                newPlan.getInterval()
            );
            
            // Calculate next billing amount
            BigDecimal nextBillingAmount = newPlan.getAmount();
            
            // Calculate total impact (proration + next billing)
            BigDecimal totalImpact = prorationAmount.add(nextBillingAmount);
            
            // Calculate savings (if any)
            BigDecimal savings = BigDecimal.ZERO;
            if (prorationAmount.compareTo(BigDecimal.ZERO) < 0) {
                savings = prorationAmount.abs();
            }
            
            // Determine proration type
            String prorationType = prorationAmount.compareTo(BigDecimal.ZERO) >= 0 ? "charge" : "credit";
            
            // Build response
            Map<String, Object> calculation = new HashMap<>();
            calculation.put("fromPlanId", fromPlanId);
            calculation.put("toPlanId", toPlanId);
            calculation.put("daysRemaining", daysRemaining);
            calculation.put("currentAmount", currentPlan.getAmount());
            calculation.put("newAmount", newPlan.getAmount());
            calculation.put("prorationAmount", prorationAmount);
            calculation.put("nextBillingAmount", nextBillingAmount);
            calculation.put("totalImpact", totalImpact);
            calculation.put("savings", savings);
            calculation.put("calculationDate", java.time.LocalDateTime.now().toString());
            calculation.put("prorationType", prorationType);
            
            logger.info("║ PRORATION CALCULATION RESULT:");
            logger.info("║ - Proration Amount: ${}", prorationAmount);
            logger.info("║ - Next Billing Amount: ${}", nextBillingAmount);
            logger.info("║ - Total Impact: ${}", totalImpact);
            logger.info("║ - Proration Type: {}", prorationType);
            logger.info("╚══════════════════════════════════════════════════════════════");
            
            return ResponseWrapper.ok(calculation);
            
        } catch (Exception e) {
            logger.error("Error calculating proration: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to calculate proration: " + e.getMessage());
        }
    }



    // ==================== HELPER METHODS ====================

    /**
     * Calculate proration amount for display purposes
     */
    private BigDecimal calculateProrationAmount(BigDecimal currentAmount, BigDecimal newAmount, Integer daysRemaining, 
                                               String currentPlanInterval, String newPlanInterval) {
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ PRORATION CALCULATION DEBUG (Display Only)");
        logger.info("║ Current Amount: ${}", currentAmount);
        logger.info("║ New Amount: ${}", newAmount);
        logger.info("║ Days Remaining: {}", daysRemaining);
        logger.info("║ Current Plan Interval: {}", currentPlanInterval);
        logger.info("║ New Plan Interval: {}", newPlanInterval);
        
        // Get billing cycle days for current plan
        int currentCycleDays = getBillingCycleDays(currentPlanInterval);
        logger.info("║ Current Cycle Days: {}", currentCycleDays);
        
        // SIMPLIFIED CALCULATION: Calculate unused time ratio regardless of plan type
        BigDecimal unusedTimeRatio = BigDecimal.valueOf(daysRemaining)
            .divide(BigDecimal.valueOf(currentCycleDays), 4, BigDecimal.ROUND_HALF_UP);
        
        // Calculate credit: current plan amount × unused time ratio
        BigDecimal creditForUnusedTime = currentAmount.multiply(unusedTimeRatio);
        
        logger.info("║ SIMPLIFIED CREDIT CALCULATION:");
        logger.info("║ - Unused Time Ratio: {}", unusedTimeRatio);
        logger.info("║ - Credit for Unused Time: ${}", creditForUnusedTime);
        
        // For upgrade: credit unused time from current plan
        // For downgrade: charge for unused time on new plan
        BigDecimal prorationAmount;
        if (newAmount.compareTo(currentAmount) > 0) {
            // UPGRADE: Credit unused time from current plan
            prorationAmount = creditForUnusedTime.negate();
            logger.info("║ UPGRADE: Crediting unused time from current plan");
        } else {
            // DOWNGRADE: Charge for unused time on new plan
            prorationAmount = creditForUnusedTime;
            logger.info("║ DOWNGRADE: Charging for unused time on new plan");
        }
        
        logger.info("║ Final Proration Amount: ${}", prorationAmount);
        logger.info("╚══════════════════════════════════════════════════════════════");
        
        return prorationAmount;
    }

    /**
     * Get billing cycle days for a plan interval
     */
    private int getBillingCycleDays(String interval) {
        if (interval == null) return 30; // Default to monthly
        
        return switch (interval.toLowerCase()) {
            case "day" -> 1;
            case "week" -> 7;
            case "month" -> 30;
            case "year" -> 365;
            default -> 30; // Default to monthly
        };
    }
} 