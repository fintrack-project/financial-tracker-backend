package com.fintrack.repository.subscription;

import com.fintrack.model.subscription.ProrationCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProrationCalculationRepository extends JpaRepository<ProrationCalculation, Long> {

    /**
     * Find calculation by hash
     */
    Optional<ProrationCalculation> findByCalculationHash(String calculationHash);

    /**
     * Find calculations by plan IDs
     */
    List<ProrationCalculation> findByFromPlanIdAndToPlanId(String fromPlanId, String toPlanId);

    /**
     * Find valid (non-expired) calculations
     */
    @Query(value = "SELECT * FROM proration_calculations WHERE expires_at > :currentTime", nativeQuery = true)
    List<ProrationCalculation> findValidCalculations(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find expired calculations
     */
    @Query(value = "SELECT * FROM proration_calculations WHERE expires_at <= :currentTime", nativeQuery = true)
    List<ProrationCalculation> findExpiredCalculations(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Delete expired calculations
     */
    @Modifying
    @Query(value = "DELETE FROM proration_calculations WHERE expires_at <= :currentTime", nativeQuery = true)
    void deleteExpiredCalculations(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Check if calculation exists and is valid
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM proration_calculations WHERE calculation_hash = :hash AND expires_at > :currentTime", nativeQuery = true)
    boolean existsValidCalculation(@Param("hash") String hash, @Param("currentTime") LocalDateTime currentTime);
} 