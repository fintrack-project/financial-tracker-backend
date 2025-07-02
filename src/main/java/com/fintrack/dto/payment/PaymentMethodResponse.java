package com.fintrack.dto.payment;

import com.fintrack.model.payment.PaymentMethod;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class PaymentMethodResponse {
    private PaymentMethod paymentMethod;
    private String message;
} 