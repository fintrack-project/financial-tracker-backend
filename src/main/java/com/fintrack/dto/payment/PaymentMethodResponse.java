package com.fintrack.dto.payment;

import com.fintrack.model.payment.PaymentMethod;

public class PaymentMethodResponse {
    private PaymentMethod paymentMethod;
    private String message;

    public PaymentMethodResponse(PaymentMethod paymentMethod, String message) {
        this.paymentMethod = paymentMethod;
        this.message = message;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 