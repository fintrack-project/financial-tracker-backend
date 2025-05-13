package com.fintrack.service.subscription;

import com.fintrack.model.subscription.UserSubscription;
import com.fintrack.model.subscription.SubscriptionPlan;
import com.fintrack.repository.subscription.UserSubscriptionRepository;
import com.fintrack.service.payment.PaymentService;
import com.fintrack.repository.payment.PaymentIntentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class BaseUserSubscriptionService {
    protected static final Logger logger = LoggerFactory.getLogger(BaseUserSubscriptionService.class);
    
    protected final UserSubscriptionRepository userSubscriptionRepository;
    protected final PaymentService paymentService;
    protected final SubscriptionPlanService subscriptionPlanService;
    protected final PaymentIntentRepository paymentIntentRepository;
    
    @Value("${stripe.secret.key}")
    protected String stripeSecretKey;

    protected BaseUserSubscriptionService(
            UserSubscriptionRepository userSubscriptionRepository,
            PaymentService paymentService,
            SubscriptionPlanService subscriptionPlanService,
            PaymentIntentRepository paymentIntentRepository) {
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.paymentService = paymentService;
        this.subscriptionPlanService = subscriptionPlanService;
        this.paymentIntentRepository = paymentIntentRepository;
    }

    protected String ensureStripeCustomerExists(UUID accountId) throws StripeException {
        String customerId = accountId.toString();
        
        try {
            Customer.retrieve(customerId);
            logger.info("Found existing Stripe customer with ID: {}", customerId);
        } catch (StripeException e) {
            if (e.getCode().equals("resource_missing") && e.getMessage().contains("No such customer")) {
                logger.info("Customer not found in Stripe, creating new customer with ID: {}", customerId);
                Map<String, Object> params = new HashMap<>();
                params.put("id", customerId);
                params.put("description", "Customer for account ID: " + accountId);
                
                Customer customer = Customer.create(params);
                customerId = customer.getId();
                logger.info("Created new Stripe customer with ID: {}", customerId);
            } else {
                logger.error("Error retrieving Stripe customer: {}", e.getMessage());
                throw e;
            }
        }
        
        return customerId;
    }

    protected void updateSubscriptionStatus(UserSubscription subscription, String status) {
        subscription.setStatus(status);
        subscription.setActive("active".equals(status));
        userSubscriptionRepository.save(subscription);
        logger.info("Updated subscription status to: {} for subscription: {}", 
                status, subscription.getStripeSubscriptionId());
    }

    protected LocalDateTime calculateNextBillingDate(SubscriptionPlan plan) {
        LocalDateTime now = LocalDateTime.now();
        if (plan.getInterval().equals("year")) {
            return now.plusYears(1);
        } else {
            return now.plusMonths(1);
        }
    }

    protected void handleStripeError(StripeException e, String operation) {
        logger.error("Stripe error during {}: {}", operation, e.getMessage());
        throw new RuntimeException("Failed to " + operation + ": " + e.getMessage());
    }

    protected void validateSubscriptionExists(UserSubscription subscription, String subscriptionId) {
        if (subscription == null) {
            throw new RuntimeException("Subscription not found: " + subscriptionId);
        }
    }

    protected void validatePlanExists(SubscriptionPlan plan, String planId) {
        if (plan == null) {
            throw new RuntimeException("Plan not found: " + planId);
        }
    }

    protected void logOperation(String operation, Map<String, Object> details) {
        logger.trace("╔══════════════════════════════════════════════════════════════");
        logger.trace("║ Operation: {}", operation);
        details.forEach((key, value) -> logger.trace("║ {}: {}", key, value));
        logger.trace("╚══════════════════════════════════════════════════════════════");
    }

    protected void validateFreeSubscription(UserSubscription subscription, String operation) {
        if (subscription.getStripeSubscriptionId().startsWith("free_")) {
            logger.error("Cannot {} a free subscription", operation);
            throw new RuntimeException("Cannot " + operation + " a free subscription. Please upgrade to a paid plan first.");
        }
    }

    protected void validateSubscriptionActive(UserSubscription subscription, String operation) {
        if (!subscription.isActive()) {
            logger.error("Cannot {} an inactive subscription", operation);
            throw new RuntimeException("Cannot " + operation + " an inactive subscription");
        }
    }

    protected void validateSubscriptionNotCancelled(UserSubscription subscription, String operation) {
        if (subscription.getCancelAtPeriodEnd()) {
            logger.error("Cannot {} a subscription that is set to cancel", operation);
            throw new RuntimeException("Cannot " + operation + " a subscription that is set to cancel");
        }
    }

    protected void validatePaymentMethod(String paymentMethodId, UUID accountId) {
        if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
            paymentService.getPaymentMethods(accountId).stream()
                    .filter(pm -> pm.getStripePaymentMethodId().equals(paymentMethodId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Payment method not found for this account"));
        }
    }

    protected void updatePaymentMethod(String customerId, String paymentMethodId) throws StripeException {
        if (paymentMethodId != null && !paymentMethodId.isEmpty()) {
            Customer customer = Customer.retrieve(customerId);
            customer.update(Map.of("invoice_settings", Map.of("default_payment_method", paymentMethodId)));
            logger.info("Updated default payment method for customer: {}", customerId);
        }
    }

    protected LocalDateTime getNextBillingDateFromStripeSubscription(com.stripe.model.Subscription stripeSubscription) {
        if (stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
            Long periodEnd = stripeSubscription.getItems().getData().get(0).getCurrentPeriodEnd();
            if (periodEnd != null) {
                return LocalDateTime.ofEpochSecond(periodEnd, 0, ZoneOffset.UTC);
            }
        }
        return LocalDateTime.now().plusDays(30); // Fallback to 30 days
    }

    protected void configureStripe() {
        Stripe.apiKey = stripeSecretKey;
    }
} 