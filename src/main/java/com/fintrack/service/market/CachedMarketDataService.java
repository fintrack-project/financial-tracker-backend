package com.fintrack.service.market;

import com.fintrack.model.market.MarketData;
import com.fintrack.repository.market.MarketDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Cached market data service that provides caching layer for market data operations.
 * Uses Valkey/Redis for caching to improve performance and reduce database load.
 */
@Service
public class CachedMarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(CachedMarketDataService.class);

    private final MarketDataRepository marketDataRepository;

    public CachedMarketDataService(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    /**
     * Find market data by symbols with caching.
     * Cache key: "marketData:bySymbols:{symbols}"
     * TTL: 5 minutes (configured in ValkeyConfig)
     */
    @Cacheable(value = "marketData", key = "'bySymbols:' + #symbols.toString()")
    public List<MarketData> findMarketDataBySymbols(List<String> symbols) {
        logger.debug("Cache miss - fetching market data for symbols: {}", symbols);
        return marketDataRepository.findMarketDataBySymbols(symbols);
    }

    /**
     * Find market data by symbol and asset type with caching.
     * Cache key: "marketData:bySymbolAndType:{symbol}:{assetType}"
     * TTL: 5 minutes (configured in ValkeyConfig)
     */
    @Cacheable(value = "marketData", key = "'bySymbolAndType:' + #symbol + ':' + #assetType")
    public List<MarketData> findMarketDataBySymbolAndAssetType(String symbol, String assetType) {
        logger.debug("Cache miss - fetching market data for symbol: {} assetType: {}", symbol, assetType);
        return marketDataRepository.findMarketDataBySymbolAndAssetType(symbol, assetType);
    }

    /**
     * Find market data by symbol with caching.
     * Cache key: "marketData:bySymbol:{symbol}"
     * TTL: 5 minutes (configured in ValkeyConfig)
     */
    @Cacheable(value = "marketData", key = "'bySymbol:' + #symbol")
    public MarketData findMarketDataBySymbol(String symbol) {
        logger.debug("Cache miss - fetching market data for symbol: {}", symbol);
        return marketDataRepository.findMarketDataBySymbol(symbol);
    }

    /**
     * Save market data and update cache.
     * Cache key: "marketData:bySymbol:{marketData.symbol}"
     * TTL: 5 minutes (configured in ValkeyConfig)
     */
    @CachePut(value = "marketData", key = "'bySymbol:' + #marketData.symbol")
    public MarketData saveMarketData(MarketData marketData) {
        logger.debug("Saving market data and updating cache for symbol: {}", marketData.getSymbol());
        return marketDataRepository.save(marketData);
    }

    /**
     * Save multiple market data entries and evict related caches.
     */
    public List<MarketData> saveAllMarketData(List<MarketData> marketDataList) {
        logger.debug("Saving {} market data entries and evicting caches", marketDataList.size());
        
        // Save to database
        List<MarketData> savedData = marketDataRepository.saveAll(marketDataList);
        
        // Evict related caches
        for (MarketData data : savedData) {
            evictMarketDataCache(data.getSymbol(), data.getAssetType().getAssetTypeName());
        }
        
        return savedData;
    }

    /**
     * Evict cache entries for a specific symbol and asset type.
     */
    @CacheEvict(value = "marketData", key = "'bySymbolAndType:' + #symbol + ':' + #assetType")
    public void evictMarketDataCache(String symbol, String assetType) {
        logger.debug("Evicting cache for symbol: {} assetType: {}", symbol, assetType);
    }

    /**
     * Evict cache entries for a specific symbol.
     */
    @CacheEvict(value = "marketData", key = "'bySymbol:' + #symbol")
    public void evictMarketDataCache(String symbol) {
        logger.debug("Evicting cache for symbol: {}", symbol);
    }

    /**
     * Evict all market data caches.
     */
    @CacheEvict(value = "marketData", allEntries = true)
    public void evictAllMarketDataCache() {
        logger.info("Evicting all market data caches");
    }

    /**
     * Check if market data exists for a symbol and asset type.
     * Uses caching to avoid repeated database calls.
     */
    @Cacheable(value = "marketData", key = "'exists:' + #symbol + ':' + #assetType")
    public boolean existsBySymbolAndAssetType(String symbol, String assetType) {
        logger.debug("Cache miss - checking existence for symbol: {} assetType: {}", symbol, assetType);
        List<MarketData> data = marketDataRepository.findMarketDataBySymbolAndAssetType(symbol, assetType);
        return !data.isEmpty();
    }

    /**
     * Get the latest market data for a symbol and asset type.
     * Uses caching to avoid repeated database calls.
     */
    @Cacheable(value = "marketData", key = "'latest:' + #symbol + ':' + #assetType")
    public Optional<MarketData> getLatestMarketData(String symbol, String assetType) {
        logger.debug("Cache miss - fetching latest market data for symbol: {} assetType: {}", symbol, assetType);
        List<MarketData> data = marketDataRepository.findMarketDataBySymbolAndAssetType(symbol, assetType);
        return data.stream()
                .max((a, b) -> a.getUpdatedAt().compareTo(b.getUpdatedAt()));
    }
} 