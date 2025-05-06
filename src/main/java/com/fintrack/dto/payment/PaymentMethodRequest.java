package com.fintrack.dto.payment;

import java.util.UUID;

public class PaymentMethodRequest {
    private UUID accountId;
    private String paymentMethodId;

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }
} 