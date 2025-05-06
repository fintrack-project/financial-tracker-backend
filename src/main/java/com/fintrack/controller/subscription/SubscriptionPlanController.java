package com.fintrack.controller.subscription;

import com.fintrack.constants.subscription.SubscriptionPlanType;
import com.fintrack.dto.subscription.SubscriptionPlanResponse;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.service.subscription.SubscriptionPlanService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription-plans")
public class SubscriptionPlanController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionPlanController.class);
    private final SubscriptionPlanService subscriptionPlanService;

    public SubscriptionPlanController(SubscriptionPlanService subscriptionPlanService) {
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionPlanResponse>> getAllPlans() {
        logger.info("Fetching all subscription plans with features");
        return ResponseEntity.ok(subscriptionPlanService.getAllPlansWithFeatures());
    }

    @GetMapping("/basic")
    public ResponseEntity<List<SubscriptionPlan>> getAllPlansBasic() {
        logger.info("Fetching all subscription plans (basic info)");
        return ResponseEntity.ok(subscriptionPlanService.getAllPlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlanById(@PathVariable String id) {
        logger.info("Fetching subscription plan by ID: {}", id);
        return subscriptionPlanService.getPlanByIdWithFeatures(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<?> getPlanByName(@PathVariable String name) {
        logger.info("Fetching subscription plan by name: {}", name);
        
        // Validate that plan name is one of the accepted values
        SubscriptionPlanType planType = SubscriptionPlanType.fromPlanName(name);
        if (planType == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid plan name: " + name,
                    "validPlans", SubscriptionPlanType.values()));
        }
        
        return subscriptionPlanService.getPlanByNameWithFeatures(planType.getPlanName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/lookup")
    public ResponseEntity<?> lookupPlanId(@RequestBody Map<String, String> request) {
        String planName = request.get("planName");
        if (planName == null || planName.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Plan name is required"));
        }
        
        // Validate that plan name is one of the accepted values
        SubscriptionPlanType planType = SubscriptionPlanType.fromPlanName(planName);
        if (planType == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid plan name: " + planName,
                    "validPlans", SubscriptionPlanType.values()));
        }
        
        logger.info("Looking up plan ID for plan name: {}", planType.getPlanName());
        try {
            String planId = subscriptionPlanService.getPlanIdByName(planType.getPlanName());
            String stripePriceId = subscriptionPlanService.getStripePriceIdByName(planType.getPlanName());
            return ResponseEntity.ok(Map.of(
                    "planId", planId,
                    "stripePriceId", stripePriceId
            ));
        } catch (RuntimeException e) {
            logger.error("Error looking up plan ID: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
} 