package com.fintrack.repository.subscription;

import com.fintrack.model.subscription.PolicyAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyAcceptanceRepository extends JpaRepository<PolicyAcceptance, Long> {

    /**
     * Find policy acceptance by account and policy type
     */
    Optional<PolicyAcceptance> findByAccountIdAndPolicyType(UUID accountId, String policyType);

    /**
     * Find policy acceptance by account, policy type and version
     */
    Optional<PolicyAcceptance> findByAccountIdAndPolicyTypeAndPolicyVersion(
            UUID accountId, String policyType, String policyVersion);

    /**
     * Find all acceptances for an account
     */
    List<PolicyAcceptance> findByAccountIdOrderByAcceptedAtDesc(UUID accountId);

    /**
     * Find all acceptances for a specific policy type and version
     */
    List<PolicyAcceptance> findByPolicyTypeAndPolicyVersion(String policyType, String policyVersion);

    /**
     * Check if user has accepted a specific policy
     */
    boolean existsByAccountIdAndPolicyTypeAndPolicyVersion(UUID accountId, String policyType, String policyVersion);

    /**
     * Find acceptances within a date range
     */
    @Query(value = "SELECT * FROM policy_acceptances WHERE accepted_at BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<PolicyAcceptance> findByAcceptedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Find the latest acceptance for a policy type by account
     */
    @Query(value = "SELECT * FROM policy_acceptances WHERE account_id = :accountId AND policy_type = :policyType ORDER BY accepted_at DESC LIMIT 1", nativeQuery = true)
    Optional<PolicyAcceptance> findLatestByAccountIdAndPolicyType(@Param("accountId") UUID accountId, 
                                                                  @Param("policyType") String policyType);
} 