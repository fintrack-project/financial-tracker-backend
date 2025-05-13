package com.fintrack.dto.payment;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentIntentRequest {
    private UUID accountId;
    private BigDecimal amount;
    private String currency;
    private String returnUrl;  // URL to return to after payment completion
} 