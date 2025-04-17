package com.fintrack.repository;

import com.fintrack.model.HoldingDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface HoldingsRepository extends JpaRepository<HoldingDto, Long> {

    @Query(value = """
        SELECT h.asset_name, h.symbol, h.total_balance, h.unit
        FROM holdings h
        WHERE h.account_id = :accountId
        AND h.date = (
            SELECT MAX(h2.date)
            FROM holdings h2
            WHERE h2.account_id = :accountId AND h2.asset_name = h.asset_name
        )
        AND h.total_balance > 0
        """, nativeQuery = true)
    List<HoldingDto> findLatestHoldingsByAccount(@Param("accountId") UUID accountId);
}