package com.fintrack.dto.subscription;

import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubscriptionPlanRequest {
    private UUID accountId;
    private String planId;
    private String paymentMethodId;
    private String returnUrl;  // URL to return to after payment completion
} 