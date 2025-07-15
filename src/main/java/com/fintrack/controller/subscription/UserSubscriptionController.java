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
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.stripe.Stripe;
import com.stripe.model.Subscription;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import java.util.HashMap;

@RestController
@RequestMapping(value = "/api/user/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserSubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionController.class);
    private final UserSubscriptionService userSubscriptionService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    public UserSubscriptionController(
            UserSubscriptionService userSubscriptionService,
            SubscriptionPlanService subscriptionPlanService,
            UserSubscriptionRepository userSubscriptionRepository) {
        this.userSubscriptionService = userSubscriptionService;
        this.subscriptionPlanService = subscriptionPlanService;
        this.userSubscriptionRepository = userSubscriptionRepository;
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

    @PostMapping("/upgrade")
    public ResponseEntity<ApiResponse<SubscriptionUpdateResponse>> upgradeUserSubscription(@RequestBody SubscriptionPlanRequest request) {
        logger.info("Upgrading subscription for account: {} with plan ID: {}", request.getAccountId(), request.getPlanId());
        
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
            
            SubscriptionUpdateResponse response = userSubscriptionService.upgradeSubscriptionWithPayment(
                    request.getAccountId(), 
                    request.getPlanId(), 
                    request.getPaymentMethodId(),
                    request.getReturnUrl());
            
            return ResponseWrapper.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error upgrading subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest("Payment processing error: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error upgrading subscription: {}", e.getMessage());
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
        String stripeSubscriptionId = requestBody.get("stripeSubscriptionId");
        if (stripeSubscriptionId == null || stripeSubscriptionId.isEmpty()) {
            return ResponseWrapper.badRequest("Stripe subscription ID is required");
        }

        try {
            SubscriptionUpdateResponse response = userSubscriptionService.cancelSubscription(stripeSubscriptionId);
            return ResponseWrapper.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error canceling subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest("Error canceling subscription: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error canceling subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/reactivate")
    public ResponseEntity<ApiResponse<SubscriptionUpdateResponse>> reactivateSubscription(@RequestBody Map<String, String> requestBody) {
        String stripeSubscriptionId = requestBody.get("stripeSubscriptionId");
        if (stripeSubscriptionId == null || stripeSubscriptionId.isEmpty()) {
            return ResponseWrapper.badRequest("Stripe subscription ID is required");
        }

        try {
            SubscriptionUpdateResponse response = userSubscriptionService.reactivateSubscription(stripeSubscriptionId);
            return ResponseWrapper.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error reactivating subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest("Error reactivating subscription: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error reactivating subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    /**
     * Manual sync endpoint to update subscription status from Stripe
     * This is useful for debugging when webhooks might not be working
     */
    @PostMapping("/sync-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncSubscriptionStatus(@RequestBody Map<String, String> requestBody) {
        String stripeSubscriptionId = requestBody.get("stripeSubscriptionId");
        if (stripeSubscriptionId == null || stripeSubscriptionId.isEmpty()) {
            return ResponseWrapper.badRequest("Stripe subscription ID is required");
        }

        try {
            logger.info("Manual sync requested for subscription: {}", stripeSubscriptionId);
            
            // Get subscription from Stripe
            Stripe.apiKey = stripeSecretKey;
            com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            
            logger.info("Stripe subscription status: {}", stripeSubscription.getStatus());
            
            // Update our database
            userSubscriptionService.handleSubscriptionUpdated(
                stripeSubscriptionId,
                stripeSubscription.getStatus(),
                stripeSubscription.getCancelAtPeriodEnd()
            );
            
            // Get updated subscription from our database
            UserSubscription dbSubscription = userSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found in database: " + stripeSubscriptionId));
            
            Map<String, Object> result = new HashMap<>();
            result.put("stripeStatus", stripeSubscription.getStatus());
            result.put("databaseStatus", dbSubscription.getStatus());
            result.put("isActive", dbSubscription.isActive());
            result.put("cancelAtPeriodEnd", stripeSubscription.getCancelAtPeriodEnd());
            result.put("lastPaymentDate", dbSubscription.getLastPaymentDate());
            result.put("nextBillingDate", dbSubscription.getNextBillingDate());
            
            logger.info("Sync completed successfully. Stripe: {}, Database: {}", 
                stripeSubscription.getStatus(), dbSubscription.getStatus());
            
            return ResponseWrapper.ok(result);
        } catch (StripeException e) {
            logger.error("Stripe error syncing subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest("Error syncing subscription: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error syncing subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    /**
     * Get subscription status from both Stripe and database for comparison
     */
    @GetMapping("/status/{stripeSubscriptionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscriptionStatus(@PathVariable String stripeSubscriptionId) {
        try {
            logger.info("Getting status for subscription: {}", stripeSubscriptionId);
            
            Map<String, Object> result = new HashMap<>();
            
            // Get from Stripe
            try {
                Stripe.apiKey = stripeSecretKey;
                com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
                
                Map<String, Object> stripeData = new HashMap<>();
                stripeData.put("status", stripeSubscription.getStatus());
                stripeData.put("cancelAtPeriodEnd", stripeSubscription.getCancelAtPeriodEnd());
                stripeData.put("created", stripeSubscription.getCreated());
                
                // Get period info from subscription items
                if (stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
                    stripeData.put("currentPeriodStart", stripeSubscription.getItems().getData().get(0).getCurrentPeriodStart());
                    stripeData.put("currentPeriodEnd", stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd());
                }
                
                result.put("stripe", stripeData);
            } catch (StripeException e) {
                result.put("stripeError", e.getMessage());
            }
            
            // Get from database
            try {
                UserSubscription dbSubscription = userSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                    .orElse(null);
                
                if (dbSubscription != null) {
                    Map<String, Object> dbData = new HashMap<>();
                    dbData.put("status", dbSubscription.getStatus());
                    dbData.put("isActive", dbSubscription.isActive());
                    dbData.put("cancelAtPeriodEnd", dbSubscription.getCancelAtPeriodEnd());
                    dbData.put("lastPaymentDate", dbSubscription.getLastPaymentDate());
                    dbData.put("nextBillingDate", dbSubscription.getNextBillingDate());
                    dbData.put("planId", dbSubscription.getPlanId());
                    
                    result.put("database", dbData);
                } else {
                    result.put("databaseError", "Subscription not found in database");
                }
            } catch (Exception e) {
                result.put("databaseError", e.getMessage());
            }
            
            return ResponseWrapper.ok(result);
        } catch (Exception e) {
            logger.error("Error getting subscription status: {}", e.getMessage());
            return ResponseWrapper.badRequest("Error getting subscription status: " + e.getMessage());
        }
    }

    @PostMapping("/downgrade")
    public ResponseEntity<ApiResponse<SubscriptionUpdateResponse>> downgradeUserSubscription(@RequestBody SubscriptionPlanRequest request) {
        logger.info("Downgrading subscription for account: {} with plan ID: {}", request.getAccountId(), request.getPlanId());
        
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
            
            SubscriptionUpdateResponse response = userSubscriptionService.downgradeSubscriptionWithPayment(
                    request.getAccountId(), 
                    request.getPlanId()
            );
            
            return ResponseWrapper.ok(response);
        } catch (StripeException e) {
            logger.error("Stripe error downgrading subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest("Payment processing error: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error downgrading subscription: {}", e.getMessage());
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
}