package com.fintrack.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.model.market.MarketData;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.util.KafkaProducerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    private final MarketDataRepository marketDataRepository;
    private final HoldingsMonthlyRepository holdingsMonthlyRepository;
    private final KafkaProducerService kafkaProducerService;

    public MarketDataService(MarketDataRepository marketDataRepository, 
        HoldingsMonthlyRepository holdingsMonthlyRepository,
        KafkaProducerService kafkaProducerService) {
        this.marketDataRepository = marketDataRepository;
        this.holdingsMonthlyRepository = holdingsMonthlyRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public List<MarketData> fetchMarketData(UUID accountId, List<Map<String, String>> entities ) {

        logger.trace("Fetching market data for entities: {}", entities);

        // Create the payload for MARKET_DATA_UPDATE_REQUEST
        List<Map<String, String>> assets = new ArrayList<>();
        List<Object[]> symbolAssetTypePairs = new ArrayList<>();
        for (Map<String, String> entity : entities) {
            Map<String, String> asset = new HashMap<>();
            String symbol = entity.get("symbol");
            String assetType = entity.get("assetType");
            asset.put("symbol", symbol);
            asset.put("asset_type", assetType);
            assets.add(asset);
            symbolAssetTypePairs.add(new Object[]{symbol, assetType});
        }

        logger.trace("Assets: {}", assets);
        // Send a Kafka message to request an update
        sendMarketDataUpdateRequest(accountId, assets);

        // Retry mechanism to fetch data until all symbols are available
        List<MarketData> result = new ArrayList<>();
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            result.clear();

            // Fetch market data for the given symbols and asset type pairs
            // TODO : we need to optimise this query to fetch all data in one go
            List<MarketData> recentMarketData = new ArrayList<>();
            symbolAssetTypePairs.stream().forEach(pair -> {
                String symbol = (String) pair[0];
                String assetType = (String) pair[1];
                logger.info("Fetching market data for symbol: " + symbol + ", assetType: " + assetType);
                List<MarketData> marketDataList = marketDataRepository.findMarketDataBySymbolAndAssetType(symbol, assetType);
                recentMarketData.addAll(marketDataList);
            });

            if (recentMarketData.isEmpty()) {
                logger.error("No data found for symbolAssetTypePairs: " + Arrays.deepToString(symbolAssetTypePairs.toArray()));
                break; // Exit if no data is found
            }

            result.addAll(recentMarketData);

            // Check if all symbolAssetTypePairs have data
            if (result.size() == symbolAssetTypePairs.size()) {
                break;
            }

            // Wait before retrying
            try {
                Thread.sleep(1000); // Wait for 2 seconds before retrying
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Retry interrupted: " + e.getMessage());
                break;
            }

            retryCount++;
        }

        if (result.size() < symbolAssetTypePairs.size()) {
            logger.error("Failed to fetch data for all symbolAssetTypePairs after " + maxRetries + " retries.");
        }

        return result;
    }

    public void sendMarketDataUpdateRequest(UUID accountId, List<Map<String, String>> assets) {
        try {    
            // Group the assets by asset type for better handling
            Map<String, List<Map<String, String>>> assetsByType = new HashMap<>();
            
            for (Map<String, String> asset : assets) {
                String assetType = asset.get("asset_type");
                assetsByType.computeIfAbsent(assetType, k -> new ArrayList<>()).add(asset);
            }
            
            // Create a single update request with all assets
            Map<String, Object> updateRequestPayload = new HashMap<>();
            updateRequestPayload.put("assets", assets);
            
            // Convert the payload to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String updateRequestJson = objectMapper.writeValueAsString(updateRequestPayload);
            
            // Publish the JSON payload to the MARKET_DATA_UPDATE_REQUEST topic
            kafkaProducerService.publishEvent(KafkaTopics.MARKET_DATA_UPDATE_REQUEST.getTopicName(), updateRequestJson);
            logger.info("Sent market data update request: " + updateRequestJson);

            // Fetch the start_date and end_date from HoldingsMonthlyRepository
            LocalDate startDate = holdingsMonthlyRepository.findEarliestDateByAccountId(accountId);
            LocalDate endDate = holdingsMonthlyRepository.findLatestDateByAccountId(accountId);

            if (startDate == null || endDate == null) {
                logger.warn("No holdings found for accountId: " + accountId + ". Skipping MARKET_DATA_MONTHLY_REQUEST.");
                return;
            }
            
            // Use current date as end_date if it's later than endDate
            LocalDate currentDate = LocalDate.now();
            if (currentDate.isAfter(endDate)) {
                logger.debug("Using current date {} instead of {} as end_date", currentDate, endDate);
                endDate = currentDate;
            }
    
            // Create the payload for MARKET_DATA_MONTHLY_REQUEST
            Map<String, Object> monthlyRequestPayload = new HashMap<>();
            monthlyRequestPayload.put("assets", assets);
            monthlyRequestPayload.put("start_date", startDate.toString());
            monthlyRequestPayload.put("end_date", endDate.toString());
    
            // Convert the payload to a JSON string
            String monthlyRequestJson = objectMapper.writeValueAsString(monthlyRequestPayload);
    
            // Publish the JSON payload to the MARKET_DATA_MONTHLY_REQUEST topic
            kafkaProducerService.publishEvent(KafkaTopics.HISTORICAL_MARKET_DATA_REQUEST.getTopicName(), monthlyRequestJson);
            logger.info("Sent market data monthly request: " + monthlyRequestJson);
        } catch (Exception e) {
            logger.error("Failed to send market data update or monthly request: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}", groupId = "market-data-group")
    public void onMarketDataUpdateComplete(String message) {
        logger.info("Received " + KafkaTopics.MARKET_DATA_UPDATE_COMPLETE.getTopicName() + " message: " + message);

        // No need to save the data; just log the message
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            logger.info("Market data update complete payload: " + payload);
        } catch (Exception e) {
            logger.error("Failed to process market data update complete message: " + e.getMessage());
        }
    }
}