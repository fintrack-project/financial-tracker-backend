package com.fintrack.repository.market;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.fintrack.model.market.MarketData;

import org.springframework.data.repository.query.Param;

import java.util.*;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    @Query(value = "SELECT * FROM market_data WHERE symbol IN :symbols", nativeQuery = true)
    List<MarketData> findMarketDataBySymbols(@Param("symbols") List<String> symbols);

    @Query(value = "SELECT * FROM market_data WHERE symbol = :symbol AND asset_type = :assetType", nativeQuery = true)
    List<MarketData> findMarketDataBySymbolAndAssetType(@Param("symbol") String symbol, @Param("assetType") String assetType);

    @Query(value = "SELECT * FROM market_data WHERE symbol = :symbol", nativeQuery = true)
    MarketData findMarketDataBySymbol(@Param("symbol") String symbol);
}