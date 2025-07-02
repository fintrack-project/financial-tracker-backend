package com.fintrack.dto.subscription;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ConfirmPaymentRequest {
    private String paymentIntentId;
    private String subscriptionId;
} 