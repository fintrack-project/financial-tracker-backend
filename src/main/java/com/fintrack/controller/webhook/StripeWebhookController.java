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
import java.util.Map;

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
                    
                case "invoice.paid":
                    handleInvoicePaid(event);
                    break;
                    
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                    
                case "invoice.payment_action_required":
                    handleInvoicePaymentActionRequired(event);
                    break;
                    
                case "invoice.created":
                    handleInvoiceCreated(event);
                    break;
                    
                case "invoice.finalized":
                    handleInvoiceFinalized(event);
                    break;
                    
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                    
                case "charge.succeeded":
                    handleChargeSucceeded(event);
                    break;
                    
                case "charge.updated":
                    handleChargeUpdated(event);
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
        try {
            StripeObject stripeObject = event.getData().getObject();
            if (stripeObject instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObject;
                logger.info("║ Payment Intent Succeeded");
                logger.info("║ - ID: {}", paymentIntent.getId());
                logger.info("║ - Amount: {}", paymentIntent.getAmount());
                logger.info("║ - Status: {}", paymentIntent.getStatus());
                
                // Update payment intent in our database
                Optional<PaymentIntent> dbPaymentIntent = 
                    paymentIntentRepository.findByStripePaymentIntentId(paymentIntent.getId());
                if (dbPaymentIntent.isPresent()) {
                    PaymentIntent intent = dbPaymentIntent.get();
                    intent.setStatus(paymentIntent.getStatus());
                    paymentIntentRepository.save(intent);
                    logger.info("✓ Payment intent status updated in database");
                }

                // If this payment intent is for a subscription, update the subscription status
                String subscriptionId = paymentIntent.getMetadata().get("subscription_id");
                if (subscriptionId != null) {
                    com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(subscriptionId);
                    userSubscriptionService.handleSubscriptionUpdated(
                        subscriptionId,
                        subscription.getStatus(),
                        subscription.getCancelAtPeriodEnd()
                    );
                    logger.info("✓ Subscription status updated after payment success: {}", subscription.getStatus());
                }
            }
        } catch (Exception e) {
            logger.error("Error handling payment_intent.succeeded: ", e);
            throw new RuntimeException("Error handling payment intent success", e);
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

    private void handleInvoicePaid(Event event) {
        try {
            StripeObject invoiceObject = event.getData().getObject();
            if (invoiceObject instanceof com.stripe.model.Invoice) {
                com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) invoiceObject;
                
                // Get subscription ID from invoice lines
                String subscriptionId = null;
                if (invoice.getLines() != null && invoice.getLines().getData() != null) {
                    for (com.stripe.model.InvoiceLineItem line : invoice.getLines().getData()) {
                        if (line.getSubscription() != null) {
                            subscriptionId = line.getSubscription();
                            break;
                        }
                    }
                }
                
                if (subscriptionId != null) {
                    // Get the subscription from Stripe to get the latest status
                    com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(subscriptionId);
                    
                    // Update subscription status in our database
                    userSubscriptionService.handleSubscriptionUpdated(
                        subscriptionId,
                        subscription.getStatus(),
                        subscription.getCancelAtPeriodEnd()
                    );
                    
                    logger.info("Invoice paid for subscription: {}, new status: {}", subscriptionId, subscription.getStatus());
                }
            }
        } catch (Exception e) {
            logger.error("Error handling invoice.paid: ", e);
            throw new RuntimeException("Error handling invoice payment", e);
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        try {
            StripeObject invoiceObject = event.getData().getObject();
            if (invoiceObject instanceof com.stripe.model.Invoice) {
                com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) invoiceObject;
                
                // Get subscription ID from invoice lines
                String subscriptionId = null;
                if (invoice.getLines() != null && invoice.getLines().getData() != null) {
                    for (com.stripe.model.InvoiceLineItem line : invoice.getLines().getData()) {
                        if (line.getSubscription() != null) {
                            subscriptionId = line.getSubscription();
                            break;
                        }
                    }
                }
                
                if (subscriptionId != null) {
                    // Get the subscription from Stripe to get the latest status
                    com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(subscriptionId);
                    
                    // Update subscription status in our database
                    userSubscriptionService.handleSubscriptionUpdated(
                        subscriptionId,
                        subscription.getStatus(),
                        subscription.getCancelAtPeriodEnd()
                    );
                    
                    logger.info("Invoice payment failed for subscription: {}, new status: {}", 
                        subscriptionId, subscription.getStatus());
                }
            }
        } catch (Exception e) {
            logger.error("Error handling invoice.payment_failed: ", e);
            throw new RuntimeException("Error handling invoice payment failure", e);
        }
    }

    private void handleInvoicePaymentActionRequired(Event event) {
        try {
            StripeObject invoiceObject = event.getData().getObject();
            if (invoiceObject instanceof com.stripe.model.Invoice) {
                com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) invoiceObject;
                
                // Get subscription ID from invoice lines
                String subscriptionId = null;
                if (invoice.getLines() != null && invoice.getLines().getData() != null) {
                    for (com.stripe.model.InvoiceLineItem line : invoice.getLines().getData()) {
                        if (line.getSubscription() != null) {
                            subscriptionId = line.getSubscription();
                            break;
                        }
                    }
                }
                
                if (subscriptionId != null) {
                    // Get the subscription from Stripe to get the latest status
                    com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(subscriptionId);
                    
                    // Update subscription status in our database
                    userSubscriptionService.handleSubscriptionUpdated(
                        subscriptionId,
                        subscription.getStatus(),
                        subscription.getCancelAtPeriodEnd()
                    );
                    
                    logger.info("Invoice payment action required for subscription: {}, new status: {}", 
                        subscriptionId, subscription.getStatus());
                }
            }
        } catch (Exception e) {
            logger.error("Error handling invoice.payment_action_required: ", e);
            throw new RuntimeException("Error handling invoice payment action requirement", e);
        }
    }

    private void handleInvoiceCreated(Event event) {
        try {
            StripeObject invoiceObject = event.getData().getObject();
            if (invoiceObject instanceof com.stripe.model.Invoice) {
                com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) invoiceObject;
                logger.info("Invoice created: {}, status: {}", invoice.getId(), invoice.getStatus());
            }
        } catch (Exception e) {
            logger.error("Error handling invoice.created: ", e);
        }
    }

    private void handleInvoiceFinalized(Event event) {
        try {
            StripeObject invoiceObject = event.getData().getObject();
            if (invoiceObject instanceof com.stripe.model.Invoice) {
                com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) invoiceObject;
                logger.info("Invoice finalized: {}, status: {}", invoice.getId(), invoice.getStatus());
            }
        } catch (Exception e) {
            logger.error("Error handling invoice.finalized: ", e);
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        try {
            StripeObject invoiceObject = event.getData().getObject();
            if (invoiceObject instanceof com.stripe.model.Invoice) {
                com.stripe.model.Invoice invoice = (com.stripe.model.Invoice) invoiceObject;
                
                // Get subscription ID from invoice lines
                String subscriptionId = null;
                if (invoice.getLines() != null && invoice.getLines().getData() != null) {
                    for (com.stripe.model.InvoiceLineItem line : invoice.getLines().getData()) {
                        if (line.getSubscription() != null) {
                            subscriptionId = line.getSubscription();
                            break;
                        }
                    }
                }
                
                if (subscriptionId != null) {
                    // Get the subscription from Stripe to get the latest status
                    com.stripe.model.Subscription subscription = com.stripe.model.Subscription.retrieve(subscriptionId);
                    
                    // Update subscription status in our database
                    userSubscriptionService.handleSubscriptionUpdated(
                        subscriptionId,
                        subscription.getStatus(),
                        subscription.getCancelAtPeriodEnd()
                    );
                    
                    logger.info("Invoice payment succeeded for subscription: {}, new status: {}", 
                        subscriptionId, subscription.getStatus());
                }
            }
        } catch (Exception e) {
            logger.error("Error handling invoice.payment_succeeded: ", e);
            throw new RuntimeException("Error handling invoice payment success", e);
        }
    }

    private void handleChargeSucceeded(Event event) {
        try {
            StripeObject chargeObject = event.getData().getObject();
            if (chargeObject instanceof com.stripe.model.Charge) {
                com.stripe.model.Charge charge = (com.stripe.model.Charge) chargeObject;
                logger.info("Charge succeeded: {}, amount: {}, status: {}", 
                    charge.getId(), charge.getAmount(), charge.getStatus());
            }
        } catch (Exception e) {
            logger.error("Error handling charge.succeeded: ", e);
        }
    }

    private void handleChargeUpdated(Event event) {
        try {
            StripeObject chargeObject = event.getData().getObject();
            if (chargeObject instanceof com.stripe.model.Charge) {
                com.stripe.model.Charge charge = (com.stripe.model.Charge) chargeObject;
                logger.info("Charge updated: {}, amount: {}, status: {}", 
                    charge.getId(), charge.getAmount(), charge.getStatus());
            }
        } catch (Exception e) {
            logger.error("Error handling charge.updated: ", e);
        }
    }
} 