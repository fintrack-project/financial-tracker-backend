package com.fintrack.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Kafka listener service for handling market data cache invalidation.
 * Listens for market data update completion messages and invalidates relevant caches.
 */
@Service
public class MarketDataCacheListener {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataCacheListener.class);

    private final CachedMarketDataService cachedMarketDataService;
    private final ObjectMapper objectMapper;

    public MarketDataCacheListener(CachedMarketDataService cachedMarketDataService, ObjectMapper objectMapper) {
        this.cachedMarketDataService = cachedMarketDataService;
        this.objectMapper = objectMapper;
    }

    /**
     * Listen for market data update completion messages and invalidate relevant caches.
     * This replaces the retry mechanism with immediate cache invalidation.
     */
    @KafkaListener(
        topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMarketDataUpdateComplete(String message) {
        try {
            logger.info("Received market data update completion message: {}", message);
            
            // Parse the completion message
            Map<String, Object> completionData = objectMapper.readValue(message, Map.class);
            
            // Extract the updated assets from the completion message
            @SuppressWarnings("unchecked")
            List<Map<String, String>> updatedAssets = (List<Map<String, String>>) completionData.get("assets");
            
            if (updatedAssets != null && !updatedAssets.isEmpty()) {
                logger.info("Invalidating cache for {} updated assets", updatedAssets.size());
                
                // Invalidate cache for each updated asset
                for (Map<String, String> asset : updatedAssets) {
                    String symbol = asset.get("symbol");
                    String assetType = asset.get("asset_type");
                    
                    if (symbol != null && assetType != null) {
                        logger.debug("Invalidating cache for symbol: {} assetType: {}", symbol, assetType);
                        cachedMarketDataService.evictMarketDataCache(symbol, assetType);
                    }
                }
                
                logger.info("Successfully invalidated cache for {} assets", updatedAssets.size());
            } else {
                logger.warn("No assets found in market data update completion message");
            }
            
        } catch (Exception e) {
            logger.error("Error processing market data update completion message: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen for market data refresh messages and invalidate all market data caches.
     * This is useful for periodic cache refresh operations.
     */
    @KafkaListener(
        topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleMarketDataRefresh(String message) {
        try {
            logger.info("Received market data refresh message: {}", message);
            
            // Invalidate all market data caches
            cachedMarketDataService.evictAllMarketDataCache();
            
            logger.info("Successfully invalidated all market data caches");
            
        } catch (Exception e) {
            logger.error("Error processing market data refresh message: {}", e.getMessage(), e);
        }
    }
} 