package com.fintrack.controller.subscription;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.dto.subscription.*;
import com.fintrack.model.subscription.*;
import com.fintrack.service.subscription.SubscriptionPolicyService;
import com.fintrack.service.subscription.SubscriptionPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/subscription-policies", produces = MediaType.APPLICATION_JSON_VALUE)
public class SubscriptionPolicyController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionPolicyController.class);
    private final SubscriptionPolicyService subscriptionPolicyService;
    private final SubscriptionPlanService subscriptionPlanService;

    public SubscriptionPolicyController(SubscriptionPolicyService subscriptionPolicyService, 
                                       SubscriptionPlanService subscriptionPlanService) {
        this.subscriptionPolicyService = subscriptionPolicyService;
        this.subscriptionPlanService = subscriptionPlanService;
    }

    // ==================== CORE API ENDPOINTS ====================

    /**
     * GET /api/subscription-policies/current - Get current policy
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<SubscriptionPolicyResponse>> getCurrentPolicy(@RequestParam(required = false) String policyType) {
        try {
            // If no policy type specified, return the general policy
            String type = policyType != null ? policyType : "general";
            
            Optional<SubscriptionPolicy> policy = subscriptionPolicyService.getActivePolicyByType(type);
            if (policy.isPresent()) {
                return ResponseWrapper.ok(SubscriptionPolicyResponse.fromSubscriptionPolicy(policy.get()));
            } else {
                return ResponseWrapper.notFound("Policy not found for type: " + type);
            }
        } catch (Exception e) {
            logger.error("Error fetching current policy: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to fetch current policy: " + e.getMessage());
        }
    }

    /**
     * POST /api/subscription-policies/accept - Record policy acceptance
     */
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<PolicyAcceptanceResponse>> acceptPolicy(@RequestBody PolicyAcceptanceRequest request) {
        try {
            PolicyAcceptance acceptance = subscriptionPolicyService.acceptPolicy(
                request.getAccountId(),
                request.getPolicyVersion(),
                request.getPolicyType(),
                request.getIpAddress(),
                request.getUserAgent()
            );
            return ResponseWrapper.ok(PolicyAcceptanceResponse.fromPolicyAcceptance(acceptance));
        } catch (Exception e) {
            logger.error("Error accepting policy: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to accept policy: " + e.getMessage());
        }
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
            
            // Calculate next billing amount (this is what the user will pay for the new plan)
            BigDecimal nextBillingAmount = newPlan.getAmount();
            
            // Calculate total impact (new plan amount + proration adjustment)
            // For upgrades: newAmount + prorationAmount (prorationAmount is negative, so this subtracts the credit)
            // For downgrades: newAmount + prorationAmount (prorationAmount is negative, so this subtracts the credit)
            BigDecimal totalImpact = nextBillingAmount.add(prorationAmount);
            
            // Calculate savings (how much they save compared to paying full price for both plans)
            BigDecimal savings = BigDecimal.ZERO;
            if (currentPlan.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Only calculate savings if they had a paid plan
                savings = currentPlan.getAmount().subtract(totalImpact);
            }
            
            // Create comprehensive response
            Map<String, Object> prorationResult = new HashMap<>();
            prorationResult.put("fromPlanId", fromPlanId);
            prorationResult.put("toPlanId", toPlanId);
            prorationResult.put("daysRemaining", daysRemaining);
            prorationResult.put("currentAmount", currentPlan.getAmount());
            prorationResult.put("newAmount", newPlan.getAmount());
            prorationResult.put("currentInterval", currentPlan.getInterval());
            prorationResult.put("newInterval", newPlan.getInterval());
            prorationResult.put("prorationAmount", prorationAmount);
            prorationResult.put("nextBillingAmount", nextBillingAmount);
            prorationResult.put("totalImpact", totalImpact);
            prorationResult.put("savings", savings);
            prorationResult.put("calculationDate", java.time.LocalDateTime.now());
            prorationResult.put("prorationType", prorationAmount.compareTo(BigDecimal.ZERO) > 0 ? "charge" : "credit");
            
            logger.info("║ DISPLAY PRORATION RESULT:");
            logger.info("║ - Proration Amount: ${} (credit for unused time)", prorationAmount);
            logger.info("║ - Next Billing Amount: ${} (new plan full price)", nextBillingAmount);
            logger.info("║ - Total Impact: ${} (what user pays today)", totalImpact);
            logger.info("║ - Savings: ${} (compared to paying full price)", savings);
            logger.info("║ - Proration Type: {}", prorationAmount.compareTo(BigDecimal.ZERO) > 0 ? "charge" : "credit");
            logger.info("║ - Note: This is for display only. Stripe will calculate the final amount.");
            logger.info("╚══════════════════════════════════════════════════════════════");
            
            return ResponseWrapper.ok(prorationResult);
        } catch (Exception e) {
            logger.error("Error calculating proration: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to calculate proration: " + e.getMessage());
        }
    }

    /**
     * GET /api/subscription-policies/audit/{accountId} - Get change history
     */
    @GetMapping("/audit/{accountId}")
    public ResponseEntity<ApiResponse<List<SubscriptionChangeAuditResponse>>> getChangeHistory(@PathVariable UUID accountId) {
        try {
            List<SubscriptionChangeAudit> audits = subscriptionPolicyService.getUserSubscriptionHistory(accountId);
            List<SubscriptionChangeAuditResponse> responses = audits.stream()
                .map(SubscriptionChangeAuditResponse::fromSubscriptionChangeAudit)
                .collect(Collectors.toList());
            return ResponseWrapper.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching change history for account {}: {}", accountId, e.getMessage());
            return ResponseWrapper.badRequest("Failed to fetch change history: " + e.getMessage());
        }
    }

    // ==================== ADDITIONAL UTILITY ENDPOINTS ====================

    /**
     * GET /api/subscription-policies/all - Get all active policies (for admin use)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SubscriptionPolicyResponse>>> getAllActivePolicies() {
        try {
            List<SubscriptionPolicy> policies = subscriptionPolicyService.getAllActivePolicies();
            List<SubscriptionPolicyResponse> responses = policies.stream()
                .map(SubscriptionPolicyResponse::fromSubscriptionPolicy)
                .collect(Collectors.toList());
            return ResponseWrapper.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching active policies: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to fetch policies: " + e.getMessage());
        }
    }

    /**
     * GET /api/subscription-policies/acceptance/check - Check if user has accepted a policy
     */
    @GetMapping("/acceptance/check")
    public ResponseEntity<ApiResponse<Boolean>> checkPolicyAcceptance(@RequestParam UUID accountId, 
                                                                     @RequestParam String policyType,
                                                                     @RequestParam String policyVersion) {
        try {
            boolean hasAccepted = subscriptionPolicyService.hasAcceptedPolicy(accountId, policyType, policyVersion);
            return ResponseWrapper.ok(hasAccepted);
        } catch (Exception e) {
            logger.error("Error checking policy acceptance: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to check policy acceptance: " + e.getMessage());
        }
    }

    /**
     * GET /api/subscription-policies/acceptance/user/{accountId} - Get user's policy acceptances
     */
    @GetMapping("/acceptance/user/{accountId}")
    public ResponseEntity<ApiResponse<List<PolicyAcceptanceResponse>>> getUserPolicyAcceptances(@PathVariable UUID accountId) {
        try {
            List<PolicyAcceptance> acceptances = subscriptionPolicyService.getUserPolicyAcceptances(accountId);
            List<PolicyAcceptanceResponse> responses = acceptances.stream()
                .map(PolicyAcceptanceResponse::fromPolicyAcceptance)
                .collect(Collectors.toList());
            return ResponseWrapper.ok(responses);
        } catch (Exception e) {
            logger.error("Error fetching user policy acceptances: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to fetch policy acceptances: " + e.getMessage());
        }
    }

    /**
     * POST /api/subscription-policies/audit/record - Record subscription change audit
     */
    @PostMapping("/audit/record")
    public ResponseEntity<ApiResponse<SubscriptionChangeAuditResponse>> recordSubscriptionChange(@RequestBody Map<String, Object> request) {
        try {
            UUID accountId = UUID.fromString((String) request.get("accountId"));
            String changeType = (String) request.get("changeType");
            String fromPlanId = (String) request.get("fromPlanId");
            String toPlanId = (String) request.get("toPlanId");
            String policyVersion = (String) request.get("policyVersion");
            String ipAddress = (String) request.get("ipAddress");
            String userAgent = (String) request.get("userAgent");

            if (accountId == null || changeType == null) {
                return ResponseWrapper.badRequest("accountId and changeType are required");
            }

            SubscriptionChangeAudit audit = subscriptionPolicyService.recordSubscriptionChange(
                accountId, changeType, fromPlanId, toPlanId, policyVersion, ipAddress, userAgent
            );
            return ResponseWrapper.ok(SubscriptionChangeAuditResponse.fromSubscriptionChangeAudit(audit));
        } catch (Exception e) {
            logger.error("Error recording subscription change: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to record subscription change: " + e.getMessage());
        }
    }

    /**
     * GET /api/subscription-policies/stats - Get policy statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPolicyStatistics() {
        try {
            Map<String, Object> stats = subscriptionPolicyService.getPolicyStatistics();
            return ResponseWrapper.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching policy statistics: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to fetch policy statistics: " + e.getMessage());
        }
    }

    /**
     * GET /api/subscription-policies/health - Health check
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseWrapper.ok("Subscription Policy Service is healthy");
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
        logger.info("║ - Unused Time Ratio: {} ({} days / {} days)", unusedTimeRatio, daysRemaining, currentCycleDays);
        logger.info("║ - Credit for Unused Time: ${} (${} × {})", creditForUnusedTime, currentAmount, unusedTimeRatio);
        
        // The proration amount is the negative of the credit (what we're giving back)
        BigDecimal prorationAmount = creditForUnusedTime.negate();
        logger.info("║ Proration Amount: ${} (negative credit)", prorationAmount);
        logger.info("║ Note: This is for display only. Stripe will calculate the final amount.");
        logger.info("╚══════════════════════════════════════════════════════════════");
        
        return prorationAmount;
    }
    
    /**
     * Get billing cycle days based on interval
     */
    private int getBillingCycleDays(String interval) {
        switch (interval.toLowerCase()) {
            case "year":
            case "annual":
                return 365;
            case "month":
            case "monthly":
                return 30;
            case "week":
            case "weekly":
                return 7;
            default:
                return 30; // Default to monthly
        }
    }
} 