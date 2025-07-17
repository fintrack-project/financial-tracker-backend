package com.fintrack.dto.subscription;

import com.fintrack.model.subscription.SubscriptionPolicy;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPolicyResponse {
    private Long id;
    private String version;
    private String policyType;
    private String content;
    private LocalDateTime effectiveDate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubscriptionPolicyResponse fromSubscriptionPolicy(SubscriptionPolicy policy) {
        SubscriptionPolicyResponse response = new SubscriptionPolicyResponse();
        response.setId(policy.getId());
        response.setVersion(policy.getVersion());
        response.setPolicyType(policy.getPolicyType());
        response.setContent(policy.getContent());
        response.setEffectiveDate(policy.getEffectiveDate());
        response.setActive(policy.isActive());
        response.setCreatedAt(policy.getCreatedAt());
        response.setUpdatedAt(policy.getUpdatedAt());
        return response;
    }
} 