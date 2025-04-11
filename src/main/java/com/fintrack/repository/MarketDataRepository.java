package com.fintrack.repository;

import com.fintrack.model.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.*;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, UUID> {
    @Query("SELECT m.id, m.symbol, m.price, m.percent_change, m.timestamp, m.asset_name, m.price_unit FROM market_data m WHERE m.asset_name IN (:assetNames)")
    List<MarketData> findMarketDataByAssetNames(@Param("assetNames") List<String> assetNames);
}