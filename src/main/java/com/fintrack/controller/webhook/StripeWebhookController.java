package com.fintrack.controller.webhook;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.service.subscription.UserSubscriptionService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
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

            if ("payment_intent.succeeded".equals(event.getType())) {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
                String subscriptionId = intent.getMetadata().get("subscription_id");
                
                if (subscriptionId != null) {
                    userSubscriptionService.confirmPayment(intent.getId(), subscriptionId);
                    logger.info("Payment confirmed for subscription: {}", subscriptionId);
                    return ResponseWrapper.ok("Webhook processed successfully");
                }
            }

            return ResponseWrapper.ok("Webhook received but no action taken");
        } catch (Exception e) {
            logger.error("Error processing webhook: ", e);
            return ResponseWrapper.badRequest("Webhook error: " + e.getMessage());
        }
    }
} 