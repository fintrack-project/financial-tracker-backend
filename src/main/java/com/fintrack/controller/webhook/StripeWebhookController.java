package com.fintrack.controller.webhook;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.service.subscription.UserSubscriptionService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/webhooks")
public class StripeWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);
    
    private final UserSubscriptionService userSubscriptionService;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    public StripeWebhookController(UserSubscriptionService userSubscriptionService) {
        this.userSubscriptionService = userSubscriptionService;
    }

    @PostMapping(
        value = "/stripe",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<String>> handleStripeWebhook(
        @RequestBody String payload,
        @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            logger.info("Received Stripe webhook event: {}", event.getType());

            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                    
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                    
                case "payment_intent.requires_action":
                    handlePaymentIntentRequiresAction(event);
                    break;
                    
                case "customer.subscription.created":
                    handleSubscriptionCreated(event);
                    break;
                    
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                    
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                    
                default:
                    logger.info("Unhandled event type: {}", event.getType());
            }

            return ResponseWrapper.ok("Webhook processed successfully");
        } catch (Exception e) {
            logger.error("Error processing webhook: ", e);
            return ResponseWrapper.badRequest("Webhook error: " + e.getMessage());
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        try {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
            String subscriptionId = intent.getMetadata().get("subscription_id");
            
            if (subscriptionId != null) {
                userSubscriptionService.confirmPayment(intent.getId(), subscriptionId);
                logger.info("Payment confirmed for subscription: {}", subscriptionId);
            } else {
                logger.warn("Payment intent succeeded but no subscription_id in metadata: {}", intent.getId());
            }
        } catch (Exception e) {
            logger.error("Error handling payment_intent.succeeded: ", e);
            throw new RuntimeException("Error handling payment success", e);
        }
    }

    private void handlePaymentIntentFailed(Event event) {
        try {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
            String subscriptionId = intent.getMetadata().get("subscription_id");
            
            if (subscriptionId != null) {
                userSubscriptionService.handleFailedPayment(
                    intent.getId(), 
                    subscriptionId, 
                    intent.getLastPaymentError() != null ? intent.getLastPaymentError().getMessage() : "Payment failed"
                );
                logger.info("Payment failure handled for subscription: {}", subscriptionId);
            }
        } catch (Exception e) {
            logger.error("Error handling payment_intent.payment_failed: ", e);
            throw new RuntimeException("Error handling payment failure", e);
        }
    }

    private void handlePaymentIntentRequiresAction(Event event) {
        try {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
            String subscriptionId = intent.getMetadata().get("subscription_id");
            
            if (subscriptionId != null) {
                userSubscriptionService.handlePaymentRequiresAction(
                    intent.getId(),
                    subscriptionId,
                    intent.getNextAction() != null ? intent.getNextAction().toString() : null
                );
                logger.info("Payment requires action for subscription: {}", subscriptionId);
            }
        } catch (Exception e) {
            logger.error("Error handling payment_intent.requires_action: ", e);
            throw new RuntimeException("Error handling payment action requirement", e);
        }
    }

    private void handleSubscriptionCreated(Event event) {
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().get();
            userSubscriptionService.handleSubscriptionCreated(subscription.getId(), subscription.getStatus());
            logger.info("New subscription created: {}", subscription.getId());
        } catch (Exception e) {
            logger.error("Error handling customer.subscription.created: ", e);
            throw new RuntimeException("Error handling subscription creation", e);
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().get();
            userSubscriptionService.handleSubscriptionUpdated(
                subscription.getId(),
                subscription.getStatus(),
                subscription.getCancelAtPeriodEnd()
            );
            logger.info("Subscription updated: {}", subscription.getId());
        } catch (Exception e) {
            logger.error("Error handling customer.subscription.updated: ", e);
            throw new RuntimeException("Error handling subscription update", e);
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        try {
            Subscription subscription = (Subscription) event.getDataObjectDeserializer().getObject().get();
            userSubscriptionService.handleSubscriptionDeleted(subscription.getId());
            logger.info("Subscription deleted: {}", subscription.getId());
        } catch (Exception e) {
            logger.error("Error handling customer.subscription.deleted: ", e);
            throw new RuntimeException("Error handling subscription deletion", e);
        }
    }
} 