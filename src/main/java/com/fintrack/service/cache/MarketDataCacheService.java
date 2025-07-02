package com.fintrack.service.cache;

import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MarketDataCacheService {
    
    // Single symbol operations
    Optional<MarketData> getMarketData(String symbol, AssetType assetType);
    void setMarketData(MarketData marketData);
    void invalidateMarketData(String symbol, AssetType assetType);
    
    // Batch operations
    List<MarketData> getMarketDataBatch(List<String> symbols, AssetType assetType);
    void setMarketDataBatch(List<MarketData> marketDataList);
    
    // Account-specific operations
    List<MarketData> getAccountMarketData(UUID accountId);
    void setAccountMarketData(UUID accountId, List<MarketData> marketDataList);
    
    // Cache management
    void clearCache();
    Map<String, Object> getCacheStats();
    boolean isDataFresh(String symbol, AssetType assetType, Duration maxAge);
} 