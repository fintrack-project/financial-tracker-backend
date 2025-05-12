package com.fintrack.controller.subscription;

import com.fintrack.constants.subscription.SubscriptionPlanType;
import com.fintrack.dto.subscription.SubscriptionPlanRequest;
import com.fintrack.dto.subscription.UserSubscriptionDetailsResponse;
import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.dto.subscription.ConfirmPaymentRequest;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.service.subscription.SubscriptionPlanService;
import com.fintrack.service.subscription.UserSubscriptionService;
import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/user/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserSubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionController.class);
    private final UserSubscriptionService userSubscriptionService;
    private final SubscriptionPlanService subscriptionPlanService;

    public UserSubscriptionController(
            UserSubscriptionService userSubscriptionService,
            SubscriptionPlanService subscriptionPlanService) {
        this.userSubscriptionService = userSubscriptionService;
        this.subscriptionPlanService = subscriptionPlanService;
    }

    @PostMapping("/fetch")
    public ResponseEntity<?> fetchUserSubscriptionDetails(@RequestBody Map<String, String> requestBody) {
        String accountIdStr = requestBody.get("accountId");
        if (accountIdStr == null || accountIdStr.isEmpty()) {
            return ResponseWrapper.badRequest("Account ID is required");
        }

        try {
            UUID accountId = UUID.fromString(accountIdStr);
            Optional<UserSubscription> user = userSubscriptionService.getSubscriptionByAccountId(accountId);
            if (user.isPresent()) {
                return ResponseWrapper.ok(user.get());
            } else {
                return ResponseWrapper.notFound();
            }
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest("Invalid account ID format");
        }
    }

    @PostMapping("/details")
    public ResponseEntity<ApiResponse<UserSubscriptionDetailsResponse>> fetchUserSubscriptionWithPlanDetails(@RequestBody Map<String, String> requestBody) {
        String accountIdStr = requestBody.get("accountId");
        if (accountIdStr == null || accountIdStr.isEmpty()) {
            return ResponseWrapper.badRequest("Account ID is required");
        }

        try {
            UUID accountId = UUID.fromString(accountIdStr);
            Optional<UserSubscription> userSubscriptionOpt = userSubscriptionService.getSubscriptionByAccountId(accountId);
            
            if (userSubscriptionOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            UserSubscription subscription = userSubscriptionOpt.get();
            Optional<SubscriptionPlan> planOpt = subscriptionPlanService.getPlanById(subscription.getPlanId());
            
            if (planOpt.isEmpty()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Plan not found"));
            }
            
            SubscriptionPlan plan = planOpt.get();
            
            // Create detailed response
            UserSubscriptionDetailsResponse response = new UserSubscriptionDetailsResponse();
            response.setSubscription(subscription);
            response.setPlan(plan);
            
            return ResponseWrapper.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest("Invalid account ID format");
        }
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<SubscriptionUpdateResponse>> updateUserSubscription(@RequestBody SubscriptionPlanRequest request) {
        logger.info("Updating subscription for account: {} with plan ID: {}", request.getAccountId(), request.getPlanId());
        
        if (request.getAccountId() == null) {
            return ResponseWrapper.badRequest("Account ID is required");
        }
        
        if (request.getPlanId() == null || request.getPlanId().isEmpty()) {
            return ResponseWrapper.badRequest("Plan ID is required");
        }
        
        try {
            // Verify that the plan exists
            Optional<SubscriptionPlan> planOpt = subscriptionPlanService.getPlanById(request.getPlanId());
            if (planOpt.isEmpty()) {
                return ResponseWrapper.badRequest("Invalid plan ID: " + request.getPlanId());
            }
            
            SubscriptionUpdateResponse response = userSubscriptionService.updateSubscriptionWithPayment(
                    request.getAccountId(), 
                    request.getPlanId(), 
                    request.getPaymentMethodId(),
                    request.getReturnUrl());
            
            return ResponseWrapper.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error updating subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest("Payment processing error: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error updating subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/confirm-payment")
    public ResponseEntity<ApiResponse<SubscriptionUpdateResponse>> confirmPayment(
            @RequestBody ConfirmPaymentRequest request) {
        logger.info("Confirming payment for subscription: {} with payment intent: {}", 
            request.getSubscriptionId(), request.getPaymentIntentId());
        
        try {
            SubscriptionUpdateResponse response = userSubscriptionService.confirmPayment(
                request.getPaymentIntentId(), 
                request.getSubscriptionId());
            return ResponseWrapper.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error confirming payment: {}", e.getMessage());
            return ResponseWrapper.badRequest("Payment confirmation error: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error confirming payment: {}", e.getMessage());
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
    
    @GetMapping("/plan-types")
    public ResponseEntity<ApiResponse<SubscriptionPlanType[]>> getAvailablePlanTypes() {
        logger.info("Getting available subscription plan types");
        return ResponseWrapper.ok(SubscriptionPlanType.values());
    }

    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<SubscriptionUpdateResponse>> cancelSubscription(@RequestBody Map<String, String> requestBody) {
        String subscriptionId = requestBody.get("subscriptionId");
        if (subscriptionId == null || subscriptionId.isEmpty()) {
            return ResponseWrapper.badRequest("Subscription ID is required");
        }

        try {
            SubscriptionUpdateResponse response = userSubscriptionService.cancelSubscription(subscriptionId);
            return ResponseWrapper.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error canceling subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest("Error canceling subscription: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error canceling subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
}