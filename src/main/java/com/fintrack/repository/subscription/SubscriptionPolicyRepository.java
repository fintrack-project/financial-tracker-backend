package com.fintrack.repository.subscription;

import com.fintrack.model.subscription.SubscriptionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPolicyRepository extends JpaRepository<SubscriptionPolicy, Long> {

    /**
     * Find the current active policy by type
     */
    @Query(value = "SELECT * FROM subscription_policies WHERE policy_type = :policyType AND is_active = true ORDER BY effective_date DESC", nativeQuery = true)
    Optional<SubscriptionPolicy> findByTypeAndIsActiveTrue(@Param("policyType") String policyType);

    /**
     * Find policy by type and version
     */
    Optional<SubscriptionPolicy> findByPolicyTypeAndVersion(String policyType, String version);

    /**
     * Find all active policies
     */
    List<SubscriptionPolicy> findByActiveTrue();

    /**
     * Find all policies by type
     */
    List<SubscriptionPolicy> findByPolicyTypeOrderByEffectiveDateDesc(String policyType);

    /**
     * Find the latest version of a policy type
     */
    @Query(value = "SELECT * FROM subscription_policies WHERE policy_type = :policyType ORDER BY version DESC LIMIT 1", nativeQuery = true)
    Optional<SubscriptionPolicy> findLatestByPolicyType(@Param("policyType") String policyType);

    /**
     * Check if a policy version exists
     */
    boolean existsByPolicyTypeAndVersion(String policyType, String version);
} 