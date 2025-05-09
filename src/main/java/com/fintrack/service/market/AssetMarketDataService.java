package com.fintrack.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.util.KafkaProducerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

/**
 * Abstract base class for all asset-specific market data services.
 * Provides common functionality for handling market data operations.
 */
public abstract class AssetMarketDataService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final MarketDataRepository marketDataRepository;
    protected final HoldingsMonthlyRepository holdingsMonthlyRepository;
    protected final KafkaProducerService kafkaProducerService;

    public AssetMarketDataService(
            MarketDataRepository marketDataRepository,
            HoldingsMonthlyRepository holdingsMonthlyRepository,
            KafkaProducerService kafkaProducerService) {
        this.marketDataRepository = marketDataRepository;
        this.holdingsMonthlyRepository = holdingsMonthlyRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    /**
     * Get the asset type handled by this service.
     * 
     * @return The asset type as defined in the AssetType enum
     */
    public abstract AssetType getAssetType();

    /**
     * Process symbols for the specific asset type.
     * Subclasses should implement this to handle asset-specific processing.
     * 
     * @param symbols List of symbol strings
     * @return List of processed symbol-asset type pairs ready for request
     */
    public abstract List<Map<String, String>> processSymbols(List<String> symbols);

    /**
     * Fetch market data for the given symbols.
     * 
     * @param accountId The account ID requesting the data
     * @param symbols List of symbol strings
     * @return List of MarketData objects with the requested data
     */
    public List<MarketData> fetchMarketData(UUID accountId, List<String> symbols) {
        // Process the symbols according to the asset type
        List<Map<String, String>> assets = processSymbols(symbols);
        
        // Send the request to update market data
        sendMarketDataUpdateRequest(accountId, assets);
        
        // Fetch and return the market data with retry
        return fetchMarketDataWithRetry(assets);
    }

    /**
     * Fetch market data with retry mechanism.
     * 
     * @param assets List of asset maps containing symbol and asset_type
     * @return List of MarketData objects
     */
    protected List<MarketData> fetchMarketDataWithRetry(List<Map<String, String>> assets) {
        List<MarketData> result = new ArrayList<>();
        int maxRetries = 3;
        int retryCount = 0;
        List<Object[]> symbolAssetTypePairs = assets.stream()
                .map(asset -> new Object[]{asset.get("symbol"), asset.get("asset_type")})
                .toList();

        while (retryCount < maxRetries) {
            result.clear();

            // Fetch market data for the given symbols and asset type pairs
            List<MarketData> recentMarketData = new ArrayList<>();
            symbolAssetTypePairs.forEach(pair -> {
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
                Thread.sleep(1000); // Wait for 1 second before retrying
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

    /**
     * Send a market data update request via Kafka.
     * 
     * @param accountId The account ID requesting the update
     * @param assets List of asset maps containing symbol and asset_type
     */
    public void sendMarketDataUpdateRequest(UUID accountId, List<Map<String, String>> assets) {
        try {    
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
} 