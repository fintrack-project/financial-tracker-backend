package com.fintrack.controller.webhook;

import com.fintrack.service.subscription.UserSubscriptionService;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.fintrack.model.payment.PaymentIntent;
import com.fintrack.common.ApiResponse;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.exception.SignatureVerificationException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhook")
public class StripeWebhookController {
    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);
    
    private final UserSubscriptionService userSubscriptionService;
    private final PaymentIntentRepository paymentIntentRepository;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Autowired
    public StripeWebhookController(UserSubscriptionService userSubscriptionService, PaymentIntentRepository paymentIntentRepository) {
        this.userSubscriptionService = userSubscriptionService;
        this.paymentIntentRepository = paymentIntentRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<String>> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event = null;
        try {
            logger.info("Received webhook event with signature: {}", sigHeader);
            
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            logger.info("Successfully verified webhook signature for event: {}", event.getId());
        } catch (SignatureVerificationException e) {
            logger.error("❌ Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(false, "Webhook signature verification failed", null));
        }

        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ Processing Stripe Webhook Event");
        logger.info("║ Event Type: {}", event.getType());
        logger.info("║ Event ID: {}", event.getId());
        // logger.info("║ Event Data: {}", event.getData().getObject());
        logger.info("╚══════════════════════════════════════════════════════════════");

        try {
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

            return ResponseEntity.ok(new ApiResponse<>(true, "Webhook processed successfully", null));
        } catch (Exception e) {
            logger.error("Error processing webhook: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Webhook error: " + e.getMessage(), null));
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        StripeObject stripeObject = event.getData().getObject();
        if (stripeObject instanceof com.stripe.model.PaymentIntent) {
            com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObject;
            logger.trace("║ Payment Intent Succeeded");
            logger.trace("║ - ID: {}", paymentIntent.getId());
            logger.trace("║ - Amount: {}", paymentIntent.getAmount());
            logger.trace("║ - Status: {}", paymentIntent.getStatus());
            
            // Update payment intent in our database
            Optional<PaymentIntent> dbPaymentIntent = 
                paymentIntentRepository.findByStripePaymentIntentId(paymentIntent.getId());
            if (dbPaymentIntent.isPresent()) {
                PaymentIntent intent = dbPaymentIntent.get();
                intent.setStatus(paymentIntent.getStatus());
                paymentIntentRepository.save(intent);
                logger.trace("✓ Payment intent status updated in database");
            }
        }
    }

    private void handlePaymentIntentFailed(Event event) {
        StripeObject failedStripeObject = event.getData().getObject();
        if (failedStripeObject instanceof com.stripe.model.PaymentIntent) {
            com.stripe.model.PaymentIntent failedPaymentIntent = (com.stripe.model.PaymentIntent) failedStripeObject;
            logger.trace("║ Payment Intent Failed");
            logger.trace("║ - ID: {}", failedPaymentIntent.getId());
            logger.trace("║ - Status: {}", failedPaymentIntent.getStatus());
            logger.trace("║ - Last Payment Error: {}", failedPaymentIntent.getLastPaymentError());
            
            // Update payment intent in our database
            Optional<PaymentIntent> failedDbPaymentIntent = 
                paymentIntentRepository.findByStripePaymentIntentId(failedPaymentIntent.getId());
            if (failedDbPaymentIntent.isPresent()) {
                PaymentIntent intent = failedDbPaymentIntent.get();
                intent.setStatus(failedPaymentIntent.getStatus());
                intent.setLastPaymentError(failedPaymentIntent.getLastPaymentError() != null ? 
                    failedPaymentIntent.getLastPaymentError().getMessage() : null);
                paymentIntentRepository.save(intent);
                logger.trace("✓ Failed payment intent status updated in database");
            }
        }
    }

    private void handlePaymentIntentRequiresAction(Event event) {
        try {
            StripeObject paymentIntentObject = event.getData().getObject();
            if (paymentIntentObject instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) paymentIntentObject;
                String subscriptionId = paymentIntent.getMetadata().get("subscription_id");
                
                if (subscriptionId != null) {
                    userSubscriptionService.handlePaymentRequiresAction(
                        paymentIntent.getId(),
                        subscriptionId,
                        paymentIntent.getNextAction() != null ? paymentIntent.getNextAction().toString() : null
                    );
                    logger.info("Payment requires action for subscription: {}", subscriptionId);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling payment_intent.requires_action: ", e);
            throw new RuntimeException("Error handling payment action requirement", e);
        }
    }

    private void handleSubscriptionCreated(Event event) {
        try {
            StripeObject subscriptionObject = event.getData().getObject();
            if (subscriptionObject instanceof Subscription) {
                Subscription subscription = (Subscription) subscriptionObject;
                userSubscriptionService.handleSubscriptionCreated(subscription.getId(), subscription.getStatus());
                logger.info("New subscription created: {}", subscription.getId());
            }
        } catch (Exception e) {
            logger.error("Error handling customer.subscription.created: ", e);
            throw new RuntimeException("Error handling subscription creation", e);
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        try {
            StripeObject subscriptionObject = event.getData().getObject();
            if (subscriptionObject instanceof Subscription) {
                Subscription subscription = (Subscription) subscriptionObject;
                userSubscriptionService.handleSubscriptionUpdated(
                    subscription.getId(),
                    subscription.getStatus(),
                    subscription.getCancelAtPeriodEnd()
                );
                logger.info("Subscription updated: {}", subscription.getId());
            }
        } catch (Exception e) {
            logger.error("Error handling customer.subscription.updated: ", e);
            throw new RuntimeException("Error handling subscription update", e);
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        try {
            StripeObject subscriptionObject = event.getData().getObject();
            if (subscriptionObject instanceof Subscription) {
                Subscription subscription = (Subscription) subscriptionObject;
                userSubscriptionService.handleSubscriptionDeleted(subscription.getId());
                logger.info("Subscription deleted: {}", subscription.getId());
            }
        } catch (Exception e) {
            logger.error("Error handling customer.subscription.deleted: ", e);
            throw new RuntimeException("Error handling subscription deletion", e);
        }
    }
} 