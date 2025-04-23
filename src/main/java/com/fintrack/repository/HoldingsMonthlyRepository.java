package com.fintrack.repository;

import com.fintrack.model.HoldingsMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HoldingsMonthlyRepository extends JpaRepository<HoldingsMonthly, Long> {

    List<HoldingsMonthly> findByAccountId(UUID accountId);

    List<HoldingsMonthly> findByAccountIdAndDateAfter(UUID accountId, LocalDate date);

    List<HoldingsMonthly> findByAccountIdAndDateBetween(UUID accountId, LocalDate startDate, LocalDate endDate);
    
    @Query(value = "SELECT MIN(date) FROM holdings_monthly WHERE account_id = :accountId", nativeQuery = true)
    LocalDate findEarliestDateByAccountId(@Param("accountId") UUID accountId);

    @Query(value = "SELECT MAX(date) FROM holdings_monthly WHERE account_id = :accountId", nativeQuery = true)
    LocalDate findLatestDateByAccountId(@Param("accountId") UUID accountId);
}