package com.fintrack.model.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_intents")
public class PaymentIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "stripe_payment_intent_id", nullable = false, unique = true)
    private String stripePaymentIntentId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "payment_method_id", length = 255)
    private String paymentMethodId;

    @Column(name = "client_secret", length = 255)
    private String clientSecret;

    @Column(name = "stripe_customer_id", length = 255)
    private String stripeCustomerId;

    @Column(name = "setup_future_usage", length = 20)
    private String setupFutureUsage;

    @Column(name = "payment_method_types", length = 255)
    private String paymentMethodTypes;

    @Column(name = "next_action", columnDefinition = "TEXT")
    private String nextAction;

    @Column(name = "last_payment_error", columnDefinition = "TEXT")
    private String lastPaymentError;

    @Column(name = "cancellation_reason", length = 50)
    private String cancellationReason;

    @Column(name = "capture_method", length = 20)
    private String captureMethod;

    @Column(name = "confirmation_method", length = 20)
    private String confirmationMethod;

    @Column(name = "requires_action")
    private Boolean requiresAction;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getSetupFutureUsage() {
        return setupFutureUsage;
    }

    public void setSetupFutureUsage(String setupFutureUsage) {
        this.setupFutureUsage = setupFutureUsage;
    }

    public String getPaymentMethodTypes() {
        return paymentMethodTypes;
    }

    public void setPaymentMethodTypes(String paymentMethodTypes) {
        this.paymentMethodTypes = paymentMethodTypes;
    }

    public String getNextAction() {
        return nextAction;
    }

    public void setNextAction(String nextAction) {
        this.nextAction = nextAction;
    }

    public String getLastPaymentError() {
        return lastPaymentError;
    }

    public void setLastPaymentError(String lastPaymentError) {
        this.lastPaymentError = lastPaymentError;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCaptureMethod() {
        return captureMethod;
    }

    public void setCaptureMethod(String captureMethod) {
        this.captureMethod = captureMethod;
    }

    public String getConfirmationMethod() {
        return confirmationMethod;
    }

    public void setConfirmationMethod(String confirmationMethod) {
        this.confirmationMethod = confirmationMethod;
    }

    public Boolean getRequiresAction() {
        return requiresAction;
    }

    public void setRequiresAction(Boolean requiresAction) {
        this.requiresAction = requiresAction;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 