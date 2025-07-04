package com.fintrack.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.service.cache.MarketDataCacheService;
import com.fintrack.util.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cached market data service that provides caching layer for market data operations.
 * Uses Valkey/Redis for caching to improve performance and reduce database load.
 */
@Service
public class CachedMarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(CachedMarketDataService.class);
    
    private final MarketDataCacheService cacheService;
    private final KafkaProducerService kafkaProducerService;
    private final MarketDataRepository marketDataRepository;
    private final ObjectMapper objectMapper;

    public CachedMarketDataService(
            MarketDataCacheService cacheService,
            KafkaProducerService kafkaProducerService,
            MarketDataRepository marketDataRepository,
            ObjectMapper objectMapper) {
        this.cacheService = cacheService;
        this.kafkaProducerService = kafkaProducerService;
        this.marketDataRepository = marketDataRepository;
        this.objectMapper = objectMapper;
    }

    public List<MarketData> fetchMarketData(UUID accountId, List<String> symbols) {
        // Group symbols by asset type
        Map<AssetType, List<String>> symbolsByAssetType = groupSymbolsByAssetType(symbols);
        List<MarketData> allResults = new ArrayList<>();

        for (Map.Entry<AssetType, List<String>> entry : symbolsByAssetType.entrySet()) {
            AssetType assetType = entry.getKey();
            List<String> assetSymbols = entry.getValue();

            // 1. Check cache first
            List<MarketData> cachedData = cacheService.getMarketDataBatch(assetSymbols, assetType);
            
            // 2. Identify missing or stale data
            List<String> missingSymbols = identifyMissingSymbols(assetSymbols, cachedData);
            List<String> staleSymbols = identifyStaleSymbols(cachedData, Duration.ofMinutes(2));

            List<String> symbolsNeedingUpdate = new ArrayList<>();
            symbolsNeedingUpdate.addAll(missingSymbols);
            symbolsNeedingUpdate.addAll(staleSymbols);

            if (!symbolsNeedingUpdate.isEmpty()) {
                logger.info("Requesting fresh data for {} symbols (missing: {}, stale: {})", 
                    symbolsNeedingUpdate.size(), missingSymbols.size(), staleSymbols.size());
                
                // 3. Send Kafka request for fresh data
                sendMarketDataUpdateRequest(accountId, symbolsNeedingUpdate, assetType);
            }

            // 4. Return cached data (even if some is stale)
            allResults.addAll(cachedData);

            // 5. If we have missing data, try to get it from database as fallback
            if (!missingSymbols.isEmpty()) {
                List<MarketData> fallbackData = fetchFromDatabase(missingSymbols, assetType);
                allResults.addAll(fallbackData);
                
                // Cache the fallback data
                if (!fallbackData.isEmpty()) {
                    cacheService.setMarketDataBatch(fallbackData);
                }
            }
        }

        logger.info("Returning {} market data entries for {} symbols", allResults.size(), symbols.size());
        return allResults;
    }

    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}", 
                  groupId = "cached-market-data-group")
    public void onMarketDataUpdateComplete(String message) {
        logger.info("Received market data update complete message from ETL batch processing");
        
        try {
            // Parse the message and extract updated market data
            Map<String, Object> payload = parseMarketDataMessage(message);
            
            // Handle single completion message with all batch results
            if (payload.containsKey("assets")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> assets = (List<Map<String, Object>>) payload.get("assets");
                
                // Convert to MarketData objects and update cache in single batch
                List<MarketData> marketDataList = convertToMarketData(assets);
                
                if (!marketDataList.isEmpty()) {
                    // Single bulk cache update for all processed transactions
                    cacheService.setMarketDataBatch(marketDataList);
                    
                    logger.info("Successfully updated cache with {} market data entries from ETL batch processing", 
                               marketDataList.size());
                    
                    // Log performance metrics
                    logBatchProcessingMetrics(marketDataList.size(), payload);
                    
                    // Optionally notify connected clients via WebSocket
                    notifyClientsOfUpdate(marketDataList);
                } else {
                    logger.warn("No market data entries found in completion message");
                }
            } else if (payload.containsKey("error")) {
                // Handle ETL processing errors
                String error = (String) payload.get("error");
                logger.error("ETL batch processing failed: {}", error);
                handleETLProcessingError(payload);
            } else {
                logger.warn("Unexpected message format in market data update completion: {}", payload.keySet());
            }
        } catch (Exception e) {
            logger.error("Error processing market data update complete message", e);
            // Consider implementing retry logic or dead letter queue
        }
    }

    private Map<AssetType, List<String>> groupSymbolsByAssetType(List<String> symbols) {
        // This is a simplified implementation - you might want to enhance this
        // based on your symbol naming conventions or asset type detection logic
        return symbols.stream()
            .collect(Collectors.groupingBy(
                symbol -> detectAssetType(symbol),
                Collectors.toList()
            ));
    }

    private AssetType detectAssetType(String symbol) {
        // Implement asset type detection logic based on your symbol conventions
        // This is a simplified example
        if (symbol.contains("/")) {
            return AssetType.FOREX;
        } else if (symbol.length() <= 4) {
            return AssetType.STOCK;
        } else {
            return AssetType.CRYPTO;
        }
    }

    private List<String> identifyMissingSymbols(List<String> symbols, List<MarketData> cachedData) {
        Set<String> cachedSymbols = cachedData.stream()
            .map(MarketData::getSymbol)
            .collect(Collectors.toSet());
        
        return symbols.stream()
            .filter(symbol -> !cachedSymbols.contains(symbol))
            .collect(Collectors.toList());
    }

    private List<String> identifyStaleSymbols(List<MarketData> cachedData, Duration maxAge) {
        return cachedData.stream()
            .filter(data -> !cacheService.isDataFresh(data.getSymbol(), data.getAssetType(), maxAge))
            .map(MarketData::getSymbol)
            .collect(Collectors.toList());
    }

    private void sendMarketDataUpdateRequest(UUID accountId, List<String> symbols, AssetType assetType) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("accountId", accountId.toString());
            payload.put("symbols", symbols);
            payload.put("assetType", assetType.getAssetTypeName());
            
            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaProducerService.publishEvent(KafkaTopics.MARKET_DATA_UPDATE_REQUEST.getTopicName(), jsonPayload);
            
            logger.debug("Sent market data update request for {} symbols", symbols.size());
        } catch (Exception e) {
            logger.error("Error sending market data update request", e);
        }
    }

    private List<MarketData> fetchFromDatabase(List<String> symbols, AssetType assetType) {
        List<MarketData> results = new ArrayList<>();
        for (String symbol : symbols) {
            List<MarketData> data = marketDataRepository.findMarketDataBySymbolAndAssetType(symbol, assetType.getAssetTypeName());
            results.addAll(data);
        }
        return results;
    }

    private Map<String, Object> parseMarketDataMessage(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            
            // Validate expected fields for ETL batch completion message
            if (payload.containsKey("assets") || payload.containsKey("error")) {
                logger.debug("Successfully parsed ETL batch completion message");
                return payload;
            } else {
                logger.warn("Message missing expected fields (assets or error): {}", payload.keySet());
                return payload;
            }
        } catch (Exception e) {
            logger.error("Error parsing market data message: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private List<MarketData> convertToMarketData(List<Map<String, Object>> assets) {
        List<MarketData> marketDataList = new ArrayList<>();
        for (Map<String, Object> asset : assets) {
            try {
                MarketData marketData = objectMapper.convertValue(asset, MarketData.class);
                marketDataList.add(marketData);
            } catch (Exception e) {
                logger.error("Error converting asset to MarketData: {}", asset, e);
            }
        }
        return marketDataList;
    }

    private void notifyClientsOfUpdate(List<MarketData> marketDataList) {
        // TODO: Implement WebSocket notification logic
        // This would notify connected frontend clients of data updates
        logger.debug("Would notify {} clients of market data updates", marketDataList.size());
    }

    /**
     * Log performance metrics for batch processing.
     * 
     * @param processedCount Number of market data entries processed
     * @param payload The original message payload
     */
    private void logBatchProcessingMetrics(int processedCount, Map<String, Object> payload) {
        try {
            // Extract batch processing metadata if available
            Integer totalBatches = (Integer) payload.get("totalBatches");
            Integer totalTransactions = (Integer) payload.get("totalTransactions");
            Long processingTimeMs = (Long) payload.get("processingTimeMs");
            
            logger.info("Batch processing metrics - Entries: {}, Batches: {}, Transactions: {}, Time: {}ms", 
                       processedCount, totalBatches, totalTransactions, processingTimeMs);
            
            // TODO: Send metrics to monitoring system (Prometheus, etc.)
        } catch (Exception e) {
            logger.debug("Could not extract batch processing metrics", e);
        }
    }

    /**
     * Handle ETL processing errors.
     * 
     * @param payload The error payload from ETL
     */
    private void handleETLProcessingError(Map<String, Object> payload) {
        try {
            String errorType = (String) payload.get("errorType");
            String batchId = (String) payload.get("batchId");
            Integer failedBatch = (Integer) payload.get("failedBatch");
            
            logger.error("ETL processing error - Type: {}, Batch: {}, FailedBatch: {}", 
                        errorType, batchId, failedBatch);
            
            // TODO: Implement error recovery logic
            // - Retry failed batches
            // - Notify administrators
            // - Update system status
            
        } catch (Exception e) {
            logger.error("Error handling ETL processing error", e);
        }
    }

    // Legacy methods for backward compatibility
    public List<MarketData> findMarketDataBySymbols(List<String> symbols) {
        return fetchMarketData(null, symbols);
    }

    public List<MarketData> findMarketDataBySymbolAndAssetType(String symbol, String assetType) {
        try {
            AssetType assetTypeEnum = AssetType.valueOf(assetType.toUpperCase());
            return fetchMarketData(null, List.of(symbol));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid asset type: {}", assetType);
            return new ArrayList<>();
        }
    }

    public MarketData findMarketDataBySymbol(String symbol) {
        List<MarketData> results = fetchMarketData(null, List.of(symbol));
        return results.isEmpty() ? null : results.get(0);
    }

    public MarketData saveMarketData(MarketData marketData) {
        MarketData saved = marketDataRepository.save(marketData);
        cacheService.setMarketData(saved);
        return saved;
    }

    public List<MarketData> saveAllMarketData(List<MarketData> marketDataList) {
        List<MarketData> savedData = marketDataRepository.saveAll(marketDataList);
        cacheService.setMarketDataBatch(savedData);
        return savedData;
    }

    public void evictMarketDataCache(String symbol, String assetType) {
        try {
            AssetType assetTypeEnum = AssetType.valueOf(assetType.toUpperCase());
            cacheService.invalidateMarketData(symbol, assetTypeEnum);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid asset type for cache eviction: {}", assetType);
        }
    }

    public void evictMarketDataCache(String symbol) {
        // Evict for all asset types
        for (AssetType assetType : AssetType.values()) {
            cacheService.invalidateMarketData(symbol, assetType);
        }
    }

    public void evictAllMarketDataCache() {
        cacheService.clearCache();
    }

    public boolean existsBySymbolAndAssetType(String symbol, String assetType) {
        try {
            AssetType assetTypeEnum = AssetType.valueOf(assetType.toUpperCase());
            Optional<MarketData> data = cacheService.getMarketData(symbol, assetTypeEnum);
            return data.isPresent();
        } catch (IllegalArgumentException e) {
            logger.error("Invalid asset type: {}", assetType);
            return false;
        }
    }

    public Optional<MarketData> getLatestMarketData(String symbol, String assetType) {
        try {
            AssetType assetTypeEnum = AssetType.valueOf(assetType.toUpperCase());
            return cacheService.getMarketData(symbol, assetTypeEnum);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid asset type: {}", assetType);
            return Optional.empty();
        }
    }
} 