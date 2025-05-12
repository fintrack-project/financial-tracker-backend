package com.fintrack.dto.payment;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentMethodRequest {
    private UUID accountId;
    private String paymentMethodId;
    private String billingAddress;
} 