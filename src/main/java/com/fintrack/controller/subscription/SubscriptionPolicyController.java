package com.fintrack.controller.subscription;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.dto.subscription.*;
import com.fintrack.model.subscription.*;
import com.fintrack.service.subscription.SubscriptionPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/subscription-policy", produces = MediaType.APPLICATION_JSON_VALUE)
public class SubscriptionPolicyController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionPolicyController.class);
    private final SubscriptionPolicyService subscriptionPolicyService;

    public SubscriptionPolicyController(SubscriptionPolicyService subscriptionPolicyService) {
        this.subscriptionPolicyService = subscriptionPolicyService;
    }

    // ==================== CORE API ENDPOINTS (as specified) ====================

    /**
     * GET /api/subscription-policy/current - Get current policy
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
                return ResponseWrapper.notFound();
            }
        } catch (Exception e) {
            logger.error("Error fetching current policy: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to fetch current policy: " + e.getMessage());
        }
    }

    /**
     * POST /api/subscription-policy/accept - Record policy acceptance
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
     * GET /api/subscription-policy/calculations/proration - Calculate proration
     */
    @GetMapping("/calculations/proration")
    public ResponseEntity<ApiResponse<ProrationCalculationResponse>> calculateProration(
            @RequestParam String fromPlanId,
            @RequestParam String toPlanId,
            @RequestParam Integer daysRemaining,
            @RequestParam BigDecimal prorationAmount,
            @RequestParam BigDecimal nextBillingAmount) {
        try {
            ProrationCalculation calculation = subscriptionPolicyService.calculateProration(
                fromPlanId, toPlanId, daysRemaining, prorationAmount, nextBillingAmount
            );
            return ResponseWrapper.ok(ProrationCalculationResponse.fromProrationCalculation(calculation));
        } catch (Exception e) {
            logger.error("Error calculating proration: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to calculate proration: " + e.getMessage());
        }
    }

    /**
     * GET /api/subscription-policy/calculations/billing-impact - Show billing impact
     */
    @GetMapping("/calculations/billing-impact")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBillingImpact(
            @RequestParam String fromPlanId,
            @RequestParam String toPlanId,
            @RequestParam Integer daysRemaining,
            @RequestParam BigDecimal currentAmount,
            @RequestParam BigDecimal newAmount) {
        try {
            // Calculate proration
            BigDecimal prorationAmount = calculateProrationAmount(currentAmount, newAmount, daysRemaining);
            BigDecimal nextBillingAmount = newAmount;
            
            // Create billing impact response
            Map<String, Object> billingImpact = Map.of(
                "fromPlanId", fromPlanId,
                "toPlanId", toPlanId,
                "daysRemaining", daysRemaining,
                "currentAmount", currentAmount,
                "newAmount", newAmount,
                "prorationAmount", prorationAmount,
                "nextBillingAmount", nextBillingAmount,
                "totalImpact", prorationAmount.add(nextBillingAmount),
                "savings", currentAmount.subtract(prorationAmount.add(nextBillingAmount))
            );
            
            return ResponseWrapper.ok(billingImpact);
        } catch (Exception e) {
            logger.error("Error calculating billing impact: {}", e.getMessage());
            return ResponseWrapper.badRequest("Failed to calculate billing impact: " + e.getMessage());
        }
    }

    /**
     * GET /api/subscription-policy/audit/{accountId} - Get change history
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
     * Get all active policies (for admin use)
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
     * Check if user has accepted a policy
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
     * Get user's policy acceptances
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
     * Record subscription change audit
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
     * Get policy statistics
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
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseWrapper.ok("Subscription Policy Service is healthy");
    }

    // ==================== HELPER METHODS ====================

    /**
     * Calculate proration amount based on remaining days
     */
    private BigDecimal calculateProrationAmount(BigDecimal currentAmount, BigDecimal newAmount, Integer daysRemaining) {
        // Simple proration calculation: (newAmount - currentAmount) * (daysRemaining / 30)
        // Assuming 30 days billing cycle
        BigDecimal dailyRate = newAmount.subtract(currentAmount).divide(BigDecimal.valueOf(30), 2, BigDecimal.ROUND_HALF_UP);
        return dailyRate.multiply(BigDecimal.valueOf(daysRemaining));
    }
} 