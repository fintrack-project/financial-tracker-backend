package com.fintrack.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import com.fintrack.service.market.base.AbstractMarketDataProvider;
import com.fintrack.util.KafkaProducerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Central service for managing market data operations across different asset types.
 * This service delegates to asset-specific services based on asset type.
 */
@Service
public class MarketDataService extends AbstractMarketDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    private final StockMarketDataService stockMarketDataService;
    private final ForexMarketDataService forexMarketDataService;
    private final CryptoMarketDataService cryptoMarketDataService;
    private final CommodityMarketDataService commodityMarketDataService;

    public MarketDataService(
            StockMarketDataService stockMarketDataService,
            ForexMarketDataService forexMarketDataService,
            CryptoMarketDataService cryptoMarketDataService,
            CommodityMarketDataService commodityMarketDataService,
        KafkaProducerService kafkaProducerService) {
        super(kafkaProducerService);
        this.stockMarketDataService = stockMarketDataService;
        this.forexMarketDataService = forexMarketDataService;
        this.cryptoMarketDataService = cryptoMarketDataService;
        this.commodityMarketDataService = commodityMarketDataService;
    }

    /**
     * Fetch market data for the given entities.
     * 
     * @param accountId The account ID requesting the data
     * @param entities List of entity maps containing symbol and assetType
     * @return List of MarketData objects with the requested data
     */
    public List<MarketData> fetchMarketData(UUID accountId, List<Map<String, String>> entities) {
        logger.trace("Fetching market data for entities: {}", entities);

        // Group entities by asset type
        Map<String, List<String>> entitiesByAssetType = groupEntitiesByAssetType(entities);
        
        // Process each asset type with its appropriate service
        List<MarketData> allResults = new ArrayList<>();
        
        // Process STOCK entities
        if (entitiesByAssetType.containsKey(AssetType.STOCK.getAssetTypeName())) {
            List<String> stockSymbols = entitiesByAssetType.get(AssetType.STOCK.getAssetTypeName());
            logger.info("Processing {} STOCK symbols", stockSymbols.size());
            allResults.addAll(stockMarketDataService.fetchMarketData(accountId, stockSymbols));
        }
        
        // Process FOREX entities
        if (entitiesByAssetType.containsKey(AssetType.FOREX.getAssetTypeName())) {
            List<String> forexSymbols = entitiesByAssetType.get(AssetType.FOREX.getAssetTypeName());
            logger.info("Processing {} FOREX symbols", forexSymbols.size());
            allResults.addAll(forexMarketDataService.fetchMarketData(accountId, forexSymbols));
        }
        
        // Process CRYPTO entities
        if (entitiesByAssetType.containsKey(AssetType.CRYPTO.getAssetTypeName())) {
            List<String> cryptoSymbols = entitiesByAssetType.get(AssetType.CRYPTO.getAssetTypeName());
            logger.info("Processing {} CRYPTO symbols", cryptoSymbols.size());
            allResults.addAll(cryptoMarketDataService.fetchMarketData(accountId, cryptoSymbols));
        }
        
        // Process COMMODITY entities
        if (entitiesByAssetType.containsKey(AssetType.COMMODITY.getAssetTypeName())) {
            List<String> commoditySymbols = entitiesByAssetType.get(AssetType.COMMODITY.getAssetTypeName());
            logger.info("Processing {} COMMODITY symbols", commoditySymbols.size());
            allResults.addAll(commodityMarketDataService.fetchMarketData(accountId, commoditySymbols));
        }
        
        return allResults;
    }

    /**
     * Group entities by asset type.
     * 
     * @param entities List of entity maps containing symbol and assetType
     * @return Map of asset type to list of symbols
     */
    private Map<String, List<String>> groupEntitiesByAssetType(List<Map<String, String>> entities) {
        Map<String, List<String>> entitiesByAssetType = new HashMap<>();
        
        for (Map<String, String> entity : entities) {
            String symbol = entity.get("symbol");
            String assetType = entity.get("assetType");
            
            if (symbol != null && assetType != null) {
                entitiesByAssetType
                    .computeIfAbsent(assetType, k -> new ArrayList<>())
                    .add(symbol);
            }
        }
        
        return entitiesByAssetType;
            }

    @Override
    public KafkaTopics getUpdateRequestTopic() {
        return KafkaTopics.MARKET_DATA_UPDATE_REQUEST;
        }

    @Override
    public Map<String, Object> createUpdateRequestPayload(Map<String, Object> data) {
        // Convert symbols to assets format if needed
        if (data.containsKey("symbols")) {
            List<String> symbols = (List<String>) data.get("symbols");
            
            // Create assets in the required format for the Kafka message
            List<Map<String, String>> assets = symbols.stream()
                .map(symbol -> Map.of("symbol", symbol, "asset_type", AssetType.STOCK.getAssetTypeName()))
                .collect(Collectors.toList());
            
            // Replace symbols with assets in the payload
            Map<String, Object> payload = new HashMap<>(data);
            payload.remove("symbols");
            payload.put("assets", assets);
            
            return payload;
        }
        
        return data;
    }

    @Override
    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}", groupId = "market-data-group")
    public void onMarketDataUpdateComplete(String message) {
        logger.info("Received " + KafkaTopics.MARKET_DATA_UPDATE_COMPLETE.getTopicName() + " message: " + message);

        // No need to save the data; just log the message
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            logger.info("Market data update complete payload: " + payload);
        } catch (Exception e) {
            logger.error("Failed to process market data update complete message: " + e.getMessage());
        }
    }
}