package com.fintrack.model.payment;

import com.fintrack.model.user.Account;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_intents")
@Data
@NoArgsConstructor
public class PaymentIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    @JsonIgnore
    private Account account;

    @Column(name = "stripe_payment_intent_id", nullable = false, unique = true)
    private String stripePaymentIntentId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

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

    @Column(name = "client_secret", length = 255)
    private String clientSecret;

    @Column(name = "payment_method_id", length = 255)
    private String paymentMethodId;

    // Custom setter for account to maintain the relationship
    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }
} 