package com.fintrack.repository.market;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fintrack.model.market.MarketIndexData;

import java.util.*;

@Repository
public interface MarketIndexDataRepository extends JpaRepository<MarketIndexData, Long> {

    @Query(value = """
        SELECT id, symbol, price, price_change, percent_change, price_high, price_low, updated_at
        FROM (
            SELECT m.id, m.symbol, m.price, m.price_change, m.percent_change, m.price_high, m.price_low, m.updated_at,
                ROW_NUMBER() OVER (PARTITION BY m.symbol ORDER BY m.updated_at DESC) AS row_num
            FROM market_index_data m
            WHERE m.symbol IN (:symbols)
        ) ranked
        WHERE row_num = 1;
        """, nativeQuery = true)
    List<MarketIndexData> findMarketIndexDataBySymbols(@Param("symbols") List<String> symbols);
}