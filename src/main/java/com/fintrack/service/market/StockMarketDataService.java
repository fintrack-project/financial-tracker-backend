package com.fintrack.service.market;

import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.service.market.base.AssetMarketDataProviderBase;
import com.fintrack.util.KafkaProducerService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for handling stock-specific market data operations.
 */
@Service
public class StockMarketDataService extends AssetMarketDataProviderBase {

    public StockMarketDataService(
            MarketDataRepository marketDataRepository,
            HoldingsMonthlyRepository holdingsMonthlyRepository,
            KafkaProducerService kafkaProducerService) {
        super(marketDataRepository, holdingsMonthlyRepository, kafkaProducerService);
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.STOCK;
    }

    @Override
    public List<Map<String, String>> processSymbols(List<String> symbols) {
        logger.info("Processing {} stock symbols", symbols.size());
        
        // For stock assets, we can simply use the symbols as provided
        List<Map<String, String>> assets = new ArrayList<>();
        for (String symbol : symbols) {
            Map<String, String> asset = new HashMap<>();
            asset.put("symbol", symbol);
            asset.put("asset_type", getAssetType().getAssetTypeName());
            assets.add(asset);
        }
        
        logger.debug("Processed stock symbols: {}", assets);
        return assets;
    }

    @Override
    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}", 
                  groupId = "stock-market-data-group", 
                  properties = "#{{'spring.json.value.default.type=java.util.Map'}}")
    public void onMarketDataUpdateComplete(String message) {
        logger.info("Received market data update complete message for STOCK: {}", message);
        
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            
            // Process only if it contains stock assets
            if (payload.containsKey("assets")) {
                List<Map<String, Object>> assets = (List<Map<String, Object>>) payload.get("assets");
                assets.stream()
                    .filter(asset -> getAssetType().getAssetTypeName().equals(asset.get("asset_type")))
                    .forEach(asset -> logger.debug("Processed STOCK update for: {}", asset.get("symbol")));
            }
        } catch (Exception e) {
            logger.error("Failed to process market data update complete message: {}", e.getMessage());
        }
    }
} 