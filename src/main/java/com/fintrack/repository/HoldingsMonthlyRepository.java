package com.fintrack.repository;

import com.fintrack.model.HoldingsMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HoldingsMonthlyRepository extends JpaRepository<HoldingsMonthly, Long> {

    List<HoldingsMonthly> findByAccountId(UUID accountId);

    List<HoldingsMonthly> findByAccountIdAndDateAfter(UUID accountId, LocalDate date);
}