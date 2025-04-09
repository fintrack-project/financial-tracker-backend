package com.fintrack.repository;

import com.fintrack.model.MarketAverageData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketAverageDataRepository extends JpaRepository<MarketAverageData, Long> {
    @Query(value = 
    "SELECT id, symbol, price, price_change, percent_change, price_low, price_high, timestamp \n" + //
    "FROM market_average_data\n" + //
    "WHERE symbol = :symbol\n" + //
    "ORDER BY timestamp DESC\n" + //
    "LIMIT 1;", nativeQuery = true)
    MarketAverageData findTopBySymbolOrderByTimeDesc(@Param("symbol")String symbol);
}