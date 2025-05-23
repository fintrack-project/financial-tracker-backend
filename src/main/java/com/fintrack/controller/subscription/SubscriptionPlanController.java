package com.fintrack.controller.subscription;

import com.fintrack.constants.subscription.SubscriptionPlanType;
import com.fintrack.dto.subscription.SubscriptionPlanResponse;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.service.subscription.SubscriptionPlanService;
import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/subscription-plans", produces = MediaType.APPLICATION_JSON_VALUE)
public class SubscriptionPlanController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionPlanController.class);
    private final SubscriptionPlanService subscriptionPlanService;

    public SubscriptionPlanController(SubscriptionPlanService subscriptionPlanService) {
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getAllPlans() {
        try {
            List<SubscriptionPlanResponse> plans = subscriptionPlanService.getAllPlansWithFeatures();
            return ResponseWrapper.ok(plans);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @GetMapping("/{planId}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> getPlanById(@PathVariable String planId) {
        try {
            return subscriptionPlanService.getPlanByIdWithFeatures(planId)
                .map(plan -> ResponseWrapper.ok(plan))
                .orElse(ResponseWrapper.notFound());
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @GetMapping("/basic")
    public ResponseEntity<ApiResponse<List<SubscriptionPlan>>> getAllPlansBasic() {
        logger.info("Fetching all subscription plans (basic info)");
        return ResponseWrapper.ok(subscriptionPlanService.getAllPlans());
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> getPlanByName(@PathVariable String name) {
        try {
            // Validate that plan name is one of the accepted values
            SubscriptionPlanType planType = SubscriptionPlanType.fromPlanName(name);
            if (planType == null) {
                return ResponseWrapper.badRequest("Invalid plan name: " + name);
            }
            
            return subscriptionPlanService.getPlanByNameWithFeatures(planType.getPlanName())
                .map(plan -> ResponseWrapper.ok(plan))
                .orElse(ResponseWrapper.notFound());
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
    
    @PostMapping("/lookup")
    public ResponseEntity<?> lookupPlanId(@RequestBody Map<String, String> request) {
        String planName = request.get("planName");
        if (planName == null || planName.isEmpty()) {
            return ResponseWrapper.badRequest("Plan name is required");
        }
        
        // Validate that plan name is one of the accepted values
        SubscriptionPlanType planType = SubscriptionPlanType.fromPlanName(planName);
        if (planType == null) {
            return ResponseWrapper.badRequest("Invalid plan name: " + planName);
        }
        
        logger.info("Looking up plan ID for plan name: {}", planType.getPlanName());
        try {
            String planId = subscriptionPlanService.getPlanIdByName(planType.getPlanName());
            String stripePriceId = subscriptionPlanService.getStripePriceIdByName(planType.getPlanName());
            return ResponseWrapper.ok(Map.of(
                    "planId", planId,
                    "stripePriceId", stripePriceId
            ));
        } catch (RuntimeException e) {
            logger.error("Error looking up plan ID: {}", e.getMessage());
            return ResponseWrapper.notFound();
        }
    }
} 