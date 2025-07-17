package com.fintrack.dto.subscription;

import com.fintrack.model.subscription.PolicyAcceptance;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyAcceptanceResponse {
    private Long id;
    private UUID accountId;
    private String policyVersion;
    private String policyType;
    private LocalDateTime acceptedAt;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    public static PolicyAcceptanceResponse fromPolicyAcceptance(PolicyAcceptance acceptance) {
        PolicyAcceptanceResponse response = new PolicyAcceptanceResponse();
        response.setId(acceptance.getId());
        response.setAccountId(acceptance.getAccountId());
        response.setPolicyVersion(acceptance.getPolicyVersion());
        response.setPolicyType(acceptance.getPolicyType());
        response.setAcceptedAt(acceptance.getAcceptedAt());
        response.setIpAddress(acceptance.getIpAddress());
        response.setUserAgent(acceptance.getUserAgent());
        response.setCreatedAt(acceptance.getCreatedAt());
        return response;
    }
} 