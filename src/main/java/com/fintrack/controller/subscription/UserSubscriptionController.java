package com.fintrack.controller.subscription;

import com.fintrack.constants.subscription.SubscriptionPlanType;
import com.fintrack.dto.subscription.SubscriptionPlanRequest;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.service.subscription.UserSubscriptionService;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/subscriptions")
public class UserSubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionController.class);
    private final UserSubscriptionService userSubscriptionService;

    public UserSubscriptionController(UserSubscriptionService userSubscriptionService) {
        this.userSubscriptionService = userSubscriptionService;
    }

    @PostMapping("/fetch")
    public ResponseEntity<?> fetchUserSubscriptionDetails(@RequestBody Map<String, String> requestBody) {
        String accountIdStr = requestBody.get("accountId");
        if (accountIdStr == null || accountIdStr.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Account ID is required"));
        }

        try {
            UUID accountId = UUID.fromString(accountIdStr);
            Optional<UserSubscription> user = userSubscriptionService.getSubscriptionByAccountId(accountId);
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid account ID format"));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateUserSubscription(@RequestBody SubscriptionPlanRequest request) {
        logger.info("Updating subscription for account: {} with plan name: {}", request.getAccountId(), request.getPlanName());
        
        if (request.getAccountId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Account ID is required"));
        }
        
        if (request.getPlanName() == null || request.getPlanName().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Plan name is required"));
        }
        
        // Validate that plan name is one of the accepted values
        SubscriptionPlanType planType = SubscriptionPlanType.fromPlanName(request.getPlanName());
        if (planType == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid plan name: " + request.getPlanName(), 
                    "validPlans", SubscriptionPlanType.values()));
        }
        
        try {
            UserSubscription userSubscription = userSubscriptionService.updateSubscription(
                    request.getAccountId(), 
                    request.getPlanName(), 
                    request.getPaymentMethodId());
            
            return ResponseEntity.ok(userSubscription);
        } catch (StripeException e) {
            logger.error("Stripe error updating subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Payment processing error",
                    "details", e.getMessage()
            ));
        } catch (RuntimeException e) {
            logger.error("Error updating subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/plan-types")
    public ResponseEntity<?> getAvailablePlanTypes() {
        logger.info("Getting available subscription plan types");
        return ResponseEntity.ok(SubscriptionPlanType.values());
    }
}