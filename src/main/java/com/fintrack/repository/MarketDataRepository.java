package com.fintrack.repository;

import com.fintrack.model.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.*;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, UUID> {
    @Query(value=
      "SELECT id, symbol, price, percent_change, timestamp, asset_name, price_unit\n" + //
      "FROM (\n" + //
      "    SELECT m.id, m.symbol, m.price, m.percent_change, m.timestamp, m.asset_name, m.price_unit,\n" + //
      "      ROW_NUMBER() OVER (PARTITION BY m.asset_name ORDER BY m.timestamp DESC) AS row_num\n" + //
      "    FROM market_data m\n" + //
      "    WHERE m.asset_name IN (:assetNames)\n" + //
      ") ranked\n" + //
      "WHERE row_num = 1;", 
    nativeQuery = true)
    List<MarketData> findMarketDataByAssetNames(@Param("assetNames") List<String> assetNames);
}