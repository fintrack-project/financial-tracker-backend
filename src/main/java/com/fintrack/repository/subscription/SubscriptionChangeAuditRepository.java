package com.fintrack.repository.subscription;

import com.fintrack.model.subscription.SubscriptionChangeAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionChangeAuditRepository extends JpaRepository<SubscriptionChangeAudit, Long> {

    /**
     * Find all audit entries for an account
     */
    List<SubscriptionChangeAudit> findByAccountIdOrderByChangeDateDesc(UUID accountId);

    /**
     * Find audit entries by change type
     */
    List<SubscriptionChangeAudit> findByChangeTypeOrderByChangeDateDesc(String changeType);

    /**
     * Find audit entries for an account by change type
     */
    List<SubscriptionChangeAudit> findByAccountIdAndChangeTypeOrderByChangeDateDesc(UUID accountId, String changeType);

    /**
     * Find audit entries within a date range
     */
    @Query(value = "SELECT * FROM subscription_change_audit WHERE change_date BETWEEN :startDate AND :endDate", nativeQuery = true)
    List<SubscriptionChangeAudit> findByChangeDateBetween(@Param("startDate") LocalDateTime startDate, 
                                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Find audit entries for a specific plan change
     */
    List<SubscriptionChangeAudit> findByFromPlanIdAndToPlanIdOrderByChangeDateDesc(String fromPlanId, String toPlanId);

    /**
     * Count changes by type for an account
     */
    @Query(value = "SELECT COUNT(*) FROM subscription_change_audit WHERE account_id = :accountId AND change_type = :changeType", nativeQuery = true)
    long countByAccountIdAndChangeType(@Param("accountId") UUID accountId, @Param("changeType") String changeType);

    /**
     * Find the latest change for an account
     */
    @Query(value = "SELECT * FROM subscription_change_audit WHERE account_id = :accountId ORDER BY change_date DESC LIMIT 1", nativeQuery = true)
    SubscriptionChangeAudit findLatestByAccountId(@Param("accountId") UUID accountId);
} 