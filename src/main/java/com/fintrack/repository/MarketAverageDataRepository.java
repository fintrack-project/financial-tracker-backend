package com.fintrack.repository;

import com.fintrack.model.MarketAverageData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketAverageDataRepository extends JpaRepository<MarketAverageData, Long> {
    MarketAverageData findTopBySymbolOrderByTimeDesc(String symbol);
}