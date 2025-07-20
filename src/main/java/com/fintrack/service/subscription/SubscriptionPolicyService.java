package com.fintrack.service.subscription;

import com.fintrack.model.subscription.*;
import com.fintrack.repository.subscription.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SubscriptionPolicyService {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionPolicyService.class);
    
    private final SubscriptionPolicyRepository subscriptionPolicyRepository;
    private final PolicyAcceptanceRepository policyAcceptanceRepository;
    private final SubscriptionChangeAuditRepository subscriptionChangeAuditRepository;
    
    public SubscriptionPolicyService(
            SubscriptionPolicyRepository subscriptionPolicyRepository,
            PolicyAcceptanceRepository policyAcceptanceRepository,
            SubscriptionChangeAuditRepository subscriptionChangeAuditRepository) {
        this.subscriptionPolicyRepository = subscriptionPolicyRepository;
        this.policyAcceptanceRepository = policyAcceptanceRepository;
        this.subscriptionChangeAuditRepository = subscriptionChangeAuditRepository;
    }
    
    // ==================== POLICY MANAGEMENT METHODS ====================
    
    /**
     * Get the current active policy by type
     */
    public Optional<SubscriptionPolicy> getCurrentPolicy(String policyType) {
        logger.debug("Getting current policy for type: {}", policyType);
        return subscriptionPolicyRepository.findByTypeAndIsActiveTrue(policyType);
    }
    
    /**
     * Get active policy by type (alias for getCurrentPolicy)
     */
    public Optional<SubscriptionPolicy> getActivePolicyByType(String policyType) {
        return getCurrentPolicy(policyType);
    }
    
    /**
     * Get policy by type and version
     */
    public Optional<SubscriptionPolicy> getPolicyByTypeAndVersion(String policyType, String version) {
        logger.debug("Getting policy for type: {} and version: {}", policyType, version);
        return subscriptionPolicyRepository.findByPolicyTypeAndVersion(policyType, version);
    }
    
    /**
     * Get all active policies
     */
    public List<SubscriptionPolicy> getAllActivePolicies() {
        logger.debug("Getting all active policies");
        return subscriptionPolicyRepository.findByActiveTrue();
    }
    
    /**
     * Get all policies by type
     */
    public List<SubscriptionPolicy> getPoliciesByType(String policyType) {
        logger.debug("Getting all policies for type: {}", policyType);
        return subscriptionPolicyRepository.findByPolicyTypeOrderByEffectiveDateDesc(policyType);
    }
    
    /**
     * Get the latest version of a policy type
     */
    public Optional<SubscriptionPolicy> getLatestPolicyByType(String policyType) {
        logger.debug("Getting latest policy for type: {}", policyType);
        return subscriptionPolicyRepository.findLatestByPolicyType(policyType);
    }
    
    /**
     * Create a new policy version
     */
    public SubscriptionPolicy createPolicyVersion(String policyType, String content, String version) {
        logger.info("Creating new policy version for type: {} with version: {}", policyType, version);
        
        // Deactivate current active policy
        subscriptionPolicyRepository.findByTypeAndIsActiveTrue(policyType)
                .ifPresent(currentPolicy -> {
                    currentPolicy.setActive(false);
                    subscriptionPolicyRepository.save(currentPolicy);
                });
        
        // Create new policy
        SubscriptionPolicy newPolicy = new SubscriptionPolicy();
        newPolicy.setPolicyType(policyType);
        newPolicy.setContent(content);
        newPolicy.setVersion(version);
        newPolicy.setEffectiveDate(LocalDateTime.now());
        newPolicy.setActive(true);
        
        return subscriptionPolicyRepository.save(newPolicy);
    }
    
    /**
     * Create a new policy (alias for createPolicyVersion)
     */
    public SubscriptionPolicy createPolicy(String version, String policyType, String content) {
        return createPolicyVersion(policyType, content, version);
    }
    
    /**
     * Update an existing policy
     */
    public SubscriptionPolicy updatePolicy(Long policyId, String content) {
        logger.info("Updating policy with ID: {}", policyId);
        
        SubscriptionPolicy policy = subscriptionPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found with ID: " + policyId));
        
        policy.setContent(content);
        policy.setUpdatedAt(LocalDateTime.now());
        
        return subscriptionPolicyRepository.save(policy);
    }
    
    /**
     * Update policy with content and active status
     */
    public SubscriptionPolicy updatePolicy(Long policyId, String content, Boolean active) {
        logger.info("Updating policy with ID: {}", policyId);
        
        SubscriptionPolicy policy = subscriptionPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found with ID: " + policyId));
        
        if (content != null) {
            policy.setContent(content);
        }
        if (active != null) {
            policy.setActive(active);
        }
        policy.setUpdatedAt(LocalDateTime.now());
        
        return subscriptionPolicyRepository.save(policy);
    }
    
    /**
     * Deactivate a policy
     */
    public void deactivatePolicy(Long policyId) {
        logger.info("Deactivating policy with ID: {}", policyId);
        
        SubscriptionPolicy policy = subscriptionPolicyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found with ID: " + policyId));
        
        policy.setActive(false);
        subscriptionPolicyRepository.save(policy);
    }
    
    /**
     * Check if a policy version exists
     */
    public boolean policyVersionExists(String policyType, String version) {
        return subscriptionPolicyRepository.existsByPolicyTypeAndVersion(policyType, version);
    }
    
    // ==================== POLICY ACCEPTANCE METHODS ====================
    
    /**
     * Record policy acceptance
     */
    public PolicyAcceptance acceptPolicy(UUID accountId, String policyVersion, String policyType, 
                                        String ipAddress, String userAgent) {
        logger.info("Recording policy acceptance for account: {} policy: {} version: {}", 
                   accountId, policyType, policyVersion);
        
        PolicyAcceptance acceptance = new PolicyAcceptance();
        acceptance.setAccountId(accountId);
        acceptance.setPolicyVersion(policyVersion);
        acceptance.setPolicyType(policyType);
        acceptance.setIpAddress(ipAddress);
        acceptance.setUserAgent(userAgent);
        acceptance.setAcceptedAt(LocalDateTime.now());
        
        return policyAcceptanceRepository.save(acceptance);
    }
    
    /**
     * Check if user has accepted a specific policy
     */
    public boolean hasAcceptedPolicy(UUID accountId, String policyType, String policyVersion) {
        return policyAcceptanceRepository.existsByAccountIdAndPolicyTypeAndPolicyVersion(
            accountId, policyType, policyVersion);
    }
    
    /**
     * Get user's policy acceptances
     */
    public List<PolicyAcceptance> getUserPolicyAcceptances(UUID accountId) {
        return policyAcceptanceRepository.findByAccountIdOrderByAcceptedAtDesc(accountId);
    }
    
    // ==================== AUDIT METHODS ====================
    
    /**
     * Record subscription change audit
     */
    public SubscriptionChangeAudit recordSubscriptionChange(UUID accountId, String changeType, 
                                                           String fromPlanId, String toPlanId, 
                                                           String policyVersion, String ipAddress, 
                                                           String userAgent) {
        logger.info("Recording subscription change for account: {} type: {}", accountId, changeType);
        
        SubscriptionChangeAudit audit = new SubscriptionChangeAudit();
        audit.setAccountId(accountId);
        audit.setChangeType(changeType);
        audit.setFromPlanId(fromPlanId);
        audit.setToPlanId(toPlanId);
        audit.setPolicyVersion(policyVersion);
        audit.setIpAddress(ipAddress);
        audit.setUserAgent(userAgent);
        audit.setChangeDate(LocalDateTime.now());
        
        return subscriptionChangeAuditRepository.save(audit);
    }
    
    /**
     * Get user's subscription change history
     */
    public List<SubscriptionChangeAudit> getUserSubscriptionHistory(UUID accountId) {
        return subscriptionChangeAuditRepository.findByAccountIdOrderByChangeDateDesc(accountId);
    }
    
    /**
     * Get subscription change history by type
     */
    public List<SubscriptionChangeAudit> getSubscriptionHistoryByType(String changeType) {
        return subscriptionChangeAuditRepository.findByChangeTypeOrderByChangeDateDesc(changeType);
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Get policy statistics
     */
    public Map<String, Object> getPolicyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count active policies
        List<SubscriptionPolicy> activePolicies = getAllActivePolicies();
        stats.put("activePoliciesCount", activePolicies.size());
        
        // Count policy acceptances
        long totalAcceptances = policyAcceptanceRepository.count();
        stats.put("totalPolicyAcceptances", totalAcceptances);
        
        // Count audit entries
        long totalAuditEntries = subscriptionChangeAuditRepository.count();
        stats.put("totalAuditEntries", totalAuditEntries);
        
        return stats;
    }
} 