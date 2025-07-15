package com.fintrack.service.subscription;

import com.fintrack.dto.subscription.SubscriptionUpdateResponse;
import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Service
public class UserSubscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserSubscriptionService.class);
    
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserSubscriptionUpgradeService upgradeService;
    private final UserSubscriptionCancellationService cancellationService;
    private final UserSubscriptionReactivateService reactivateService;
    private final UserSubscriptionDowngradeService downgradeService;
    private final SubscriptionPlanService subscriptionPlanService;
    private final PaymentService paymentService;
    private final PaymentIntentRepository paymentIntentRepository;

    public UserSubscriptionService(
            UserSubscriptionRepository userSubscriptionRepository,
            UserSubscriptionUpgradeService upgradeService,
            UserSubscriptionCancellationService cancellationService,
            UserSubscriptionReactivateService reactivateService,
            UserSubscriptionDowngradeService downgradeService,
            SubscriptionPlanService subscriptionPlanService,
            PaymentService paymentService,
            PaymentIntentRepository paymentIntentRepository) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.upgradeService = upgradeService;
        this.cancellationService = cancellationService;
        this.reactivateService = reactivateService;
        this.downgradeService = downgradeService;
        this.subscriptionPlanService = subscriptionPlanService;
        this.paymentService = paymentService;
        this.paymentIntentRepository = paymentIntentRepository;
    }

    public Optional<UserSubscription> getSubscriptionByAccountId(UUID accountId) {
        return userSubscriptionRepository.findByAccountId(accountId);
    }

    @Transactional
    public SubscriptionUpdateResponse upgradeSubscriptionWithPayment(
            UUID accountId, 
            String planId, 
            String paymentMethodId,
            String returnUrl) throws StripeException {
        
        logger.info("Upgrading subscription for account: {} to plan: {}", accountId, planId);
        
        // Validate the plan exists
        Optional<SubscriptionPlan> planOpt = subscriptionPlanService.getPlanById(planId);
        if (planOpt.isEmpty()) {
            throw new RuntimeException("Invalid plan ID: " + planId);
        }
        
        // Get current subscription
        UserSubscription currentSubscription = userSubscriptionRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("No subscription found for account: " + accountId));
        
        // Perform the upgrade
        return upgradeService.upgradeSubscription(accountId, planId, paymentMethodId, returnUrl);
    }

    @Transactional
    public SubscriptionUpdateResponse confirmPayment(String paymentIntentId, String stripeSubscriptionId) throws StripeException {
        logger.info("Confirming payment for subscription: {} with payment intent: {}", stripeSubscriptionId, paymentIntentId);
        
        // Get the subscription
        UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + stripeSubscriptionId));
        
        // Get the payment intent
        com.fintrack.model.payment.PaymentIntent paymentIntent = paymentIntentRepository
                .findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment intent not found: " + paymentIntentId));
        
        // Log payment intent details before confirmation
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ Payment Intent Details (Before Confirmation)");
        logger.trace("║ - ID: {}", paymentIntent.getStripePaymentIntentId());
        logger.trace("║ - Status: {}", paymentIntent.getStatus());
        logger.trace("║ - Amount: {}", paymentIntent.getAmount());
        logger.trace("║ - Currency: {}", paymentIntent.getCurrency());
        logger.trace("║ - Payment Method ID: {}", paymentIntent.getPaymentMethodId());
        logger.trace("║ - Client Secret: {}", paymentIntent.getClientSecret());
        logger.trace("║ - Requires Action: {}", paymentIntent.getStatus().equals("requires_action"));
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
        // Get updated payment intent from Stripe
        com.stripe.model.PaymentIntent stripePaymentIntent = com.stripe.model.PaymentIntent.retrieve(paymentIntentId);
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ Payment Intent Details (After Frontend Confirmation)");
        logger.trace("║ - ID: {}", stripePaymentIntent.getId());
        logger.trace("║ - Status: {}", stripePaymentIntent.getStatus());
        logger.trace("║ - Amount: {}", stripePaymentIntent.getAmount());
        logger.trace("║ - Currency: {}", stripePaymentIntent.getCurrency());
        logger.trace("║ - Payment Method Types: {}", stripePaymentIntent.getPaymentMethodTypes());
        logger.trace("║ - Client Secret: {}", stripePaymentIntent.getClientSecret());
        logger.trace("║ - Requires Action: {}", stripePaymentIntent.getStatus().equals("requires_action"));
        logger.trace("║ - Requires Confirmation: {}", stripePaymentIntent.getStatus().equals("requires_confirmation"));
        logger.trace("╚══════════════════════════════════════════════════════════════");
        
        // Update payment intent status in database
        paymentIntent.setStatus(stripePaymentIntent.getStatus());
        paymentIntentRepository.save(paymentIntent);
        
        // Update subscription status based on payment intent status
        String paymentStatus = stripePaymentIntent.getStatus();
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ Payment Confirmation Analysis");
        logger.info("║ - Payment Intent ID: {}", paymentIntentId);
        logger.info("║ - Stripe Payment Status: {}", paymentStatus);
        logger.info("║ - Current DB Subscription Status: {}", subscription.getStatus());
        logger.info("║ - Subscription ID: {}", stripeSubscriptionId);
        logger.info("╚══════════════════════════════════════════════════════════════");
        
        // CRITICAL FIX: Only mark subscription as active if payment actually succeeded
        // This prevents marking incomplete 3D Secure payments as active
        if ("succeeded".equals(paymentStatus)) {
            // Payment actually succeeded - update subscription status
            subscription.setStatus("active");
            subscription.setActive(true);
            subscription.setLastPaymentDate(java.time.LocalDateTime.now());
            subscription = userSubscriptionRepository.save(subscription);
            logger.info("✅ Payment succeeded - Updated subscription status to active: {}", stripeSubscriptionId);
            
            // CRITICAL FIX: Handle 3D Secure payment completion
            // For 3D Secure payments, the payment intent succeeds but subscription may remain incomplete
            // We need to trigger Stripe to process the subscription properly
            try {
                com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
                logger.info("🔄 Checking Stripe subscription status: {}", stripeSubscription.getStatus());
                
                if ("incomplete".equals(stripeSubscription.getStatus())) {
                    logger.info("🔄 Stripe subscription is incomplete, checking for pending invoice...");
                    
                    // Check if there's a pending invoice that needs to be paid
                    if (stripeSubscription.getLatestInvoice() != null) {
                        String invoiceId = stripeSubscription.getLatestInvoice();
                        com.stripe.model.Invoice invoice = com.stripe.model.Invoice.retrieve(invoiceId);
                        logger.info("🔄 Invoice status: {}", invoice.getStatus());
                        
                        // If invoice is open and payment intent succeeded, pay the invoice
                        if ("open".equals(invoice.getStatus()) && paymentIntent.getStatus().equals("succeeded")) {
                            logger.info("🔄 Paying invoice: {}", invoiceId);
                            try {
                                // First, try to pay the invoice with the payment method from our database
                                Optional<com.fintrack.model.payment.PaymentIntent> dbPaymentIntent = 
                                    paymentIntentRepository.findByStripePaymentIntentId(paymentIntentId);
                                if (dbPaymentIntent.isPresent() && dbPaymentIntent.get().getPaymentMethodId() != null) {
                                    Map<String, Object> payParams = new HashMap<>();
                                    payParams.put("payment_method", dbPaymentIntent.get().getPaymentMethodId());
                                    invoice.pay(payParams);
                                    logger.info("✅ Invoice paid successfully with payment method");
                                } else {
                                    throw new RuntimeException("Payment method not found for payment intent");
                                }
                            } catch (Exception payError) {
                                logger.warn("⚠️ Failed to pay invoice with payment method: {}", payError.getMessage());
                                // Try alternative approach - mark as paid out of band
                                try {
                                    logger.info("🔄 Trying to mark invoice as paid out of band...");
                                    Map<String, Object> payParams = new HashMap<>();
                                    payParams.put("paid_out_of_band", true);
                                    invoice.pay(payParams);
                                    logger.info("✅ Invoice marked as paid out of band");
                                } catch (Exception oobError) {
                                    logger.warn("⚠️ Failed to mark invoice as paid out of band: {}", oobError.getMessage());
                                    // Last resort - try to finalize the invoice
                                    try {
                                        logger.info("🔄 Trying to finalize invoice...");
                                        invoice.finalizeInvoice();
                                        logger.info("✅ Invoice finalized successfully");
                                    } catch (Exception finalizeError) {
                                        logger.warn("⚠️ Failed to finalize invoice: {}", finalizeError.getMessage());
                                    }
                                }
                            }
                        }
                    }
                } else {
                    logger.info("✅ Stripe subscription status is already correct: {}", stripeSubscription.getStatus());
                }
            } catch (Exception e) {
                logger.warn("⚠️ Failed to process Stripe subscription update: {}", e.getMessage());
                // Don't fail the entire operation if Stripe update fails
            }
            
            // Additional step: Try to trigger subscription activation by updating metadata
            try {
                logger.info("🔄 Attempting to trigger subscription activation...");
                com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
                if ("incomplete".equals(stripeSubscription.getStatus())) {
                    // Try to update the subscription to trigger status change
                    Map<String, Object> updateParams = new HashMap<>();
                    updateParams.put("metadata", Map.of("payment_confirmed", "true", "confirmed_at", java.time.LocalDateTime.now().toString()));
                    stripeSubscription.update(updateParams);
                    logger.info("✅ Subscription metadata updated to trigger activation");
                }
            } catch (Exception e) {
                logger.warn("⚠️ Failed to update subscription metadata: {}", e.getMessage());
            }
        } else if ("processing".equals(paymentStatus)) {
            // Payment is processing - keep subscription incomplete until it succeeds
            subscription.setStatus("incomplete");
            subscription.setActive(false);
            subscription = userSubscriptionRepository.save(subscription);
            logger.info("⏳ Payment processing - Keeping subscription status as incomplete: {}", stripeSubscriptionId);
        } else if ("requires_action".equals(paymentStatus)) {
            // Payment requires 3D Secure authentication - use Stripe's incomplete status
            subscription.setStatus("incomplete");
            subscription.setActive(false);
            subscription = userSubscriptionRepository.save(subscription);
            logger.info("⏳ Payment requires action - Updated subscription status to incomplete: {}", stripeSubscriptionId);
        } else {
            // Payment failed or other status - use Stripe's incomplete status
            subscription.setStatus("incomplete");
            subscription.setActive(false);
            subscription = userSubscriptionRepository.save(subscription);
            logger.warn("❌ Payment failed - Updated subscription status to incomplete: {}", stripeSubscriptionId);
        }
        
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ Final Subscription State");
        logger.info("║ - Status: {}", subscription.getStatus());
        logger.info("║ - Active: {}", subscription.isActive());
        logger.info("║ - Last Payment: {}", subscription.getLastPaymentDate());
        logger.info("╚══════════════════════════════════════════════════════════════");
        
        return SubscriptionUpdateResponse.fromUserSubscription(subscription, null, false, null, null);
    }

    @Transactional
    public SubscriptionUpdateResponse cancelSubscription(String stripeSubscriptionId) throws StripeException {
        logger.info("Canceling subscription: {}", stripeSubscriptionId);
        
        // Perform the cancellation
        return cancellationService.cancelSubscription(stripeSubscriptionId);
    }

    @Transactional
    public SubscriptionUpdateResponse reactivateSubscription(String stripeSubscriptionId) throws StripeException {
        logger.info("Reactivating subscription: {}", stripeSubscriptionId);
        
        // Delegate to the reactivate service
        return reactivateService.reactivateSubscription(stripeSubscriptionId);
    }

    @Transactional
    public SubscriptionUpdateResponse downgradeSubscriptionWithPayment(
            UUID accountId, 
            String planId) throws StripeException {
        
        logger.info("Downgrading subscription for account: {} to plan: {}", accountId, planId);
        
        // Validate the plan exists
        Optional<SubscriptionPlan> planOpt = subscriptionPlanService.getPlanById(planId);
        if (planOpt.isEmpty()) {
            throw new RuntimeException("Invalid plan ID: " + planId);
        }
        
        // Get current subscription
        UserSubscription currentSubscription = userSubscriptionRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("No subscription found for account: " + accountId));
        
        // Perform the downgrade
        return downgradeService.downgradeSubscription(accountId, planId);
    }

    @Transactional
    public void createFreeSubscription(UUID accountId, String planName) {
        // Find the free plan by name
        Optional<SubscriptionPlan> planOpt = subscriptionPlanService.getPlanById("plan_free");
        if (planOpt.isEmpty()) {
            throw new RuntimeException("Free plan not found");
        }
        SubscriptionPlan plan = planOpt.get();

        // Create a new UserSubscription
        UserSubscription subscription = new UserSubscription();
        subscription.setAccountId(accountId);
        subscription.setPlanId(plan.getId());
        subscription.setStripeSubscriptionId("free_" + accountId.toString());
        subscription.setStatus("active");
        subscription.setActive(true);
        subscription.setSubscriptionStartDate(java.time.LocalDateTime.now());
        subscription.setNextBillingDate(null);
        subscription.setCreatedAt(java.time.LocalDateTime.now());
        subscription.setCancelAtPeriodEnd(false);
        userSubscriptionRepository.save(subscription);
        logger.info("Created free subscription for account: {}", accountId);
    }

    // Stubbed webhook handler methods for StripeWebhookController
    public void handlePaymentRequiresAction(String paymentIntentId, String subscriptionId, String nextAction) {
        logger.info("[Webhook] handlePaymentRequiresAction called: paymentIntentId={}, subscriptionId={}, nextAction={}", paymentIntentId, subscriptionId, nextAction);
    }

    public void handleSubscriptionCreated(String subscriptionId, String customerId) {
        logger.info("[Webhook] handleSubscriptionCreated called: subscriptionId={}, customerId={}", subscriptionId, customerId);
    }

    public void handleSubscriptionUpdated(String subscriptionId, String status, Boolean cancelAtPeriodEnd) {
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ [Webhook] handleSubscriptionUpdated");
        logger.info("║ - Subscription ID: {}", subscriptionId);
        logger.info("║ - New Status: {}", status);
        logger.info("║ - Cancel At Period End: {}", cancelAtPeriodEnd);
        logger.info("╚══════════════════════════════════════════════════════════════");
        
        // Get the subscription from our database
        UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));
        
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ Current Database State");
        logger.info("║ - Current Status: {}", subscription.getStatus());
        logger.info("║ - Current Active: {}", subscription.isActive());
        logger.info("║ - Account ID: {}", subscription.getAccountId());
        logger.info("╚══════════════════════════════════════════════════════════════");
        
        // Update subscription status
        subscription.setStatus(status);
        subscription.setActive("active".equals(status));
        subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
        
        // If subscription is active, update last payment date
        if ("active".equals(status)) {
            subscription.setLastPaymentDate(java.time.LocalDateTime.now());
        }
        
        // Save the updated subscription
        subscription = userSubscriptionRepository.save(subscription);
        
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ Updated Database State");
        logger.info("║ - New Status: {}", subscription.getStatus());
        logger.info("║ - New Active: {}", subscription.isActive());
        logger.info("║ - Cancel At Period End: {}", subscription.getCancelAtPeriodEnd());
        logger.info("║ - Last Payment Date: {}", subscription.getLastPaymentDate());
        logger.info("╚══════════════════════════════════════════════════════════════");
    }

    public void handleSubscriptionDeleted(String subscriptionId) {
        logger.info("[Webhook] handleSubscriptionDeleted called: subscriptionId={}", subscriptionId);
    }

    /**
     * Manually sync subscription status from Stripe to our database
     * This is useful for fixing inconsistencies between Stripe and our database
     */
    @Transactional
    public void syncSubscriptionStatusFromStripe(String stripeSubscriptionId) {
        try {
            logger.info("🔄 Syncing subscription status from Stripe: {}", stripeSubscriptionId);
            
            // Get subscription from our database
            UserSubscription subscription = userSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                    .orElseThrow(() -> new RuntimeException("Subscription not found: " + stripeSubscriptionId));
            
            // Get current status from Stripe
            com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            String stripeStatus = stripeSubscription.getStatus();
            
            logger.info("╔══════════════════════════════════════════════════════════════");
            logger.info("║ Subscription Status Sync");
            logger.info("║ - Stripe Status: {}", stripeStatus);
            logger.info("║ - Database Status: {}", subscription.getStatus());
            logger.info("║ - Database Active: {}", subscription.isActive());
            logger.info("╚══════════════════════════════════════════════════════════════");
            
            // Update our database to match Stripe
            subscription.setStatus(stripeStatus);
            subscription.setActive("active".equals(stripeStatus));
            
            // Update last payment date if subscription is now active
            if ("active".equals(stripeStatus) && !subscription.isActive()) {
                subscription.setLastPaymentDate(java.time.LocalDateTime.now());
            }
            
            subscription = userSubscriptionRepository.save(subscription);
            
            logger.info("✅ Subscription status synced successfully");
            logger.info("╔══════════════════════════════════════════════════════════════");
            logger.info("║ Updated Database State");
            logger.info("║ - New Status: {}", subscription.getStatus());
            logger.info("║ - New Active: {}", subscription.isActive());
            logger.info("╚══════════════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            logger.error("❌ Failed to sync subscription status: {}", e.getMessage());
            throw new RuntimeException("Failed to sync subscription status", e);
        }
    }
}
