package com.fintrack.dto.subscription;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyAcceptanceRequest {
    private UUID accountId;
    private String policyVersion;
    private String policyType;
    private String ipAddress;
    private String userAgent;
} 