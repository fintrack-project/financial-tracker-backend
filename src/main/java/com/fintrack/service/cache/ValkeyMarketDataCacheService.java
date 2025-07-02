package com.fintrack.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ValkeyMarketDataCacheService implements MarketDataCacheService {

    private static final Logger logger = LoggerFactory.getLogger(ValkeyMarketDataCacheService.class);
    
    private final RedisTemplate<String, Object> valkeyTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String MARKET_DATA_PREFIX = "market_data:";
    private static final String ACCOUNT_MARKET_DATA_PREFIX = "account_market_data:";
    private static final String CACHE_STATS_PREFIX = "cache_stats:";
    
    public ValkeyMarketDataCacheService(RedisTemplate<String, Object> valkeyTemplate, ObjectMapper objectMapper) {
        this.valkeyTemplate = valkeyTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Cacheable(value = "marketData", key = "#symbol + ':' + #assetType")
    public Optional<MarketData> getMarketData(String symbol, AssetType assetType) {
        String key = MARKET_DATA_PREFIX + assetType + ":" + symbol;
        try {
            MarketData marketData = (MarketData) valkeyTemplate.opsForValue().get(key);
            if (marketData != null) {
                logger.debug("Cache hit for market data: {}/{}", symbol, assetType);
                incrementCacheHit(symbol, assetType);
            } else {
                logger.debug("Cache miss for market data: {}/{}", symbol, assetType);
                incrementCacheMiss(symbol, assetType);
            }
            return Optional.ofNullable(marketData);
        } catch (Exception e) {
            logger.error("Error retrieving market data from cache: {}/{}", symbol, assetType, e);
            return Optional.empty();
        }
    }

    @Override
    @CacheEvict(value = "marketData", key = "#marketData.symbol + ':' + #marketData.assetType")
    public void setMarketData(MarketData marketData) {
        String key = MARKET_DATA_PREFIX + marketData.getAssetType() + ":" + marketData.getSymbol();
        try {
            valkeyTemplate.opsForValue().set(key, marketData, Duration.ofMinutes(5));
            logger.debug("Cached market data: {}/{}", marketData.getSymbol(), marketData.getAssetType());
        } catch (Exception e) {
            logger.error("Error caching market data: {}/{}", marketData.getSymbol(), marketData.getAssetType(), e);
        }
    }

    @Override
    public List<MarketData> getMarketDataBatch(List<String> symbols, AssetType assetType) {
        List<MarketData> results = new ArrayList<>();
        List<String> keys = symbols.stream()
            .map(symbol -> MARKET_DATA_PREFIX + assetType + ":" + symbol)
            .toList();
        
        try {
            List<Object> cachedData = valkeyTemplate.opsForValue().multiGet(keys);
            for (int i = 0; i < cachedData.size(); i++) {
                if (cachedData.get(i) != null) {
                    results.add((MarketData) cachedData.get(i));
                    incrementCacheHit(symbols.get(i), assetType);
                } else {
                    incrementCacheMiss(symbols.get(i), assetType);
                }
            }
            logger.debug("Batch cache retrieval: {}/{} found, {}/{} total", 
                results.size(), symbols.size(), results.size(), symbols.size());
        } catch (Exception e) {
            logger.error("Error in batch cache retrieval", e);
        }
        
        return results;
    }

    @Override
    public void setMarketDataBatch(List<MarketData> marketDataList) {
        try {
            Map<String, MarketData> batchData = new HashMap<>();
            for (MarketData marketData : marketDataList) {
                String key = MARKET_DATA_PREFIX + marketData.getAssetType() + ":" + marketData.getSymbol();
                batchData.put(key, marketData);
            }
            
            valkeyTemplate.opsForValue().multiSet(batchData);
            
            // Set TTL for all keys
            for (String key : batchData.keySet()) {
                valkeyTemplate.expire(key, Duration.ofMinutes(5));
            }
            
            logger.debug("Batch cached {} market data entries", marketDataList.size());
        } catch (Exception e) {
            logger.error("Error in batch cache operation", e);
        }
    }

    @Override
    public List<MarketData> getAccountMarketData(UUID accountId) {
        String key = ACCOUNT_MARKET_DATA_PREFIX + accountId;
        try {
            @SuppressWarnings("unchecked")
            List<MarketData> accountData = (List<MarketData>) valkeyTemplate.opsForValue().get(key);
            return accountData != null ? accountData : new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error retrieving account market data: {}", accountId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void setAccountMarketData(UUID accountId, List<MarketData> marketDataList) {
        String key = ACCOUNT_MARKET_DATA_PREFIX + accountId;
        try {
            valkeyTemplate.opsForValue().set(key, marketDataList, Duration.ofMinutes(10));
            logger.debug("Cached account market data for: {}", accountId);
        } catch (Exception e) {
            logger.error("Error caching account market data: {}", accountId, e);
        }
    }

    @Override
    public void invalidateMarketData(String symbol, AssetType assetType) {
        String key = MARKET_DATA_PREFIX + assetType + ":" + symbol;
        try {
            valkeyTemplate.delete(key);
            logger.debug("Invalidated market data cache: {}/{}", symbol, assetType);
        } catch (Exception e) {
            logger.error("Error invalidating market data cache: {}/{}", symbol, assetType, e);
        }
    }

    @Override
    public void clearCache() {
        try {
            Set<String> keys = valkeyTemplate.keys(MARKET_DATA_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                valkeyTemplate.delete(keys);
                logger.info("Cleared {} market data cache entries", keys.size());
            }
        } catch (Exception e) {
            logger.error("Error clearing cache", e);
        }
    }

    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            // Get cache hit/miss statistics
            Set<String> hitKeys = valkeyTemplate.keys(CACHE_STATS_PREFIX + "*:hits");
            Set<String> missKeys = valkeyTemplate.keys(CACHE_STATS_PREFIX + "*:misses");
            
            long totalHits = 0;
            long totalMisses = 0;
            
            if (hitKeys != null) {
                for (String key : hitKeys) {
                    Object value = valkeyTemplate.opsForValue().get(key);
                    if (value != null) {
                        totalHits += Long.parseLong(value.toString());
                    }
                }
            }
            
            if (missKeys != null) {
                for (String key : missKeys) {
                    Object value = valkeyTemplate.opsForValue().get(key);
                    if (value != null) {
                        totalMisses += Long.parseLong(value.toString());
                    }
                }
            }
            
            stats.put("totalHits", totalHits);
            stats.put("totalMisses", totalMisses);
            stats.put("hitRate", totalHits + totalMisses > 0 ? 
                (double) totalHits / (totalHits + totalMisses) : 0.0);
            
            Set<String> marketDataKeys = valkeyTemplate.keys(MARKET_DATA_PREFIX + "*");
            stats.put("cacheSize", marketDataKeys != null ? marketDataKeys.size() : 0);
            
        } catch (Exception e) {
            logger.error("Error getting cache stats", e);
        }
        
        return stats;
    }

    @Override
    public boolean isDataFresh(String symbol, AssetType assetType, Duration maxAge) {
        String key = MARKET_DATA_PREFIX + assetType + ":" + symbol;
        try {
            Long ttl = valkeyTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl <= 0) {
                return false;
            }
            
            Duration remainingTtl = Duration.ofSeconds(ttl);
            Duration originalTtl = Duration.ofMinutes(5);
            Duration age = originalTtl.minus(remainingTtl);
            
            return age.compareTo(maxAge) <= 0;
        } catch (Exception e) {
            logger.error("Error checking data freshness: {}/{}", symbol, assetType, e);
            return false;
        }
    }

    private void incrementCacheHit(String symbol, AssetType assetType) {
        String key = CACHE_STATS_PREFIX + assetType + ":" + symbol + ":hits";
        valkeyTemplate.opsForValue().increment(key);
    }

    private void incrementCacheMiss(String symbol, AssetType assetType) {
        String key = CACHE_STATS_PREFIX + assetType + ":" + symbol + ":misses";
        valkeyTemplate.opsForValue().increment(key);
    }
} 