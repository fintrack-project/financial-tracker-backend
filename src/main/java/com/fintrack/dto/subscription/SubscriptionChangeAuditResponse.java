package com.fintrack.dto.subscription;

import com.fintrack.model.subscription.SubscriptionChangeAudit;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionChangeAuditResponse {
    private Long id;
    private UUID accountId;
    private String changeType;
    private String fromPlanId;
    private String toPlanId;
    private BigDecimal prorationAmount;
    private String policyVersion;
    private LocalDateTime changeDate;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    public static SubscriptionChangeAuditResponse fromSubscriptionChangeAudit(SubscriptionChangeAudit audit) {
        SubscriptionChangeAuditResponse response = new SubscriptionChangeAuditResponse();
        response.setId(audit.getId());
        response.setAccountId(audit.getAccountId());
        response.setChangeType(audit.getChangeType());
        response.setFromPlanId(audit.getFromPlanId());
        response.setToPlanId(audit.getToPlanId());
        response.setProrationAmount(audit.getProrationAmount());
        response.setPolicyVersion(audit.getPolicyVersion());
        response.setChangeDate(audit.getChangeDate());
        response.setIpAddress(audit.getIpAddress());
        response.setUserAgent(audit.getUserAgent());
        response.setCreatedAt(audit.getCreatedAt());
        return response;
    }
} 