package com.fintrack.repository;

import com.fintrack.model.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.*;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    @Query(value = """
        SELECT id, symbol, price, percent_change, updated_at
        FROM (
            SELECT m.id, m.symbol, m.price, m.percent_change, m.updated_at,
                ROW_NUMBER() OVER (PARTITION BY m.symbol ORDER BY m.updated_at DESC) AS row_num
            FROM market_data m
            WHERE m.symbol IN (:symbols)
        ) ranked
        WHERE row_num = 1;
        """, nativeQuery = true)
    List<MarketData> findMarketDataBySymbols(@Param("symbols") List<String> symbols);
}