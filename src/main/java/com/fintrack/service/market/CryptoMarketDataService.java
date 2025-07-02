package com.fintrack.service.market;

import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.service.market.CachedMarketDataService;
import com.fintrack.service.market.base.AssetMarketDataProviderBase;
import com.fintrack.util.KafkaProducerService;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for handling cryptocurrency-specific market data operations.
 */
@Service
public class CryptoMarketDataService extends AssetMarketDataProviderBase {

    private static final String DEFAULT_QUOTE_CURRENCY = "USD";

    public CryptoMarketDataService(
            CachedMarketDataService cachedMarketDataService,
            HoldingsMonthlyRepository holdingsMonthlyRepository,
            KafkaProducerService kafkaProducerService) {
        super(cachedMarketDataService, holdingsMonthlyRepository, kafkaProducerService);
    }

    @Override
    public AssetType getAssetType() {
        return AssetType.CRYPTO;
    }

    @Override
    public List<Map<String, String>> processSymbols(List<String> symbols) {
        logger.info("Processing {} crypto symbols", symbols.size());
        List<Map<String, String>> assets = new ArrayList<>();
        
        for (String symbol : symbols) {
            Map<String, String> asset = new HashMap<>();
            asset.put("symbol", symbol);
            asset.put("asset_type", getAssetType().getAssetTypeName());
            assets.add(asset);
        }
        
        logger.debug("Processed crypto symbols: {}", assets);
        return assets;
    }

    @Override
    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}", 
                  groupId = "crypto-market-data-group", 
                  properties = "#{{'spring.json.value.default.type=java.util.Map'}}")
    public void onMarketDataUpdateComplete(String message) {
        logger.info("Received market data update complete message for CRYPTO: {}", message);
        
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            
            // Process only if it contains crypto assets
            if (payload.containsKey("assets")) {
                List<Map<String, Object>> assets = (List<Map<String, Object>>) payload.get("assets");
                assets.stream()
                    .filter(asset -> getAssetType().getAssetTypeName().equals(asset.get("asset_type")))
                    .forEach(asset -> logger.debug("Processed CRYPTO update for: {}", asset.get("symbol")));
            }
        } catch (Exception e) {
            logger.error("Failed to process market data update complete message: {}", e.getMessage());
        }
    }
} 