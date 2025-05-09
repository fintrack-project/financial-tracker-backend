package com.fintrack.service.market.base;

import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.market.MarketDataRepository;
import com.fintrack.util.KafkaProducerService;

import java.time.LocalDate;
import java.util.*;

/**
 * Abstract base class for asset-specific market data providers.
 * Extends AbstractMarketDataProvider and adds asset-specific functionality.
 */
public abstract class AssetMarketDataProviderBase extends AbstractMarketDataProvider {

    protected final MarketDataRepository marketDataRepository;
    protected final HoldingsMonthlyRepository holdingsMonthlyRepository;

    protected AssetMarketDataProviderBase(
            MarketDataRepository marketDataRepository,
            HoldingsMonthlyRepository holdingsMonthlyRepository,
            KafkaProducerService kafkaProducerService) {
        super(kafkaProducerService);
        this.marketDataRepository = marketDataRepository;
        this.holdingsMonthlyRepository = holdingsMonthlyRepository;
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
        
        // Create the payload for market data update
        Map<String, Object> data = new HashMap<>();
        data.put("accountId", accountId.toString());
        data.put("assets", assets);
        
        // Send the market data update request (using AbstractMarketDataProvider)
        try {
            String jsonPayload = objectMapper.writeValueAsString(data);
            sendKafkaMessage(getUpdateRequestTopic().getTopicName(), jsonPayload);
            logger.info("Sent market data update request for {} symbols", symbols.size());
        } catch (Exception e) {
            logger.error("Failed to send market data update request: {}", e.getMessage());
        }
        
        // Also send a historical data request if needed
        sendHistoricalDataRequest(accountId, assets);
        
        // Fetch and return the market data with retry
        return fetchMarketDataWithRetry(assets);
    }

    /**
     * Send a historical market data request via Kafka.
     * 
     * @param accountId The account ID requesting the update
     * @param assets List of asset maps containing symbol and asset_type
     */
    protected void sendHistoricalDataRequest(UUID accountId, List<Map<String, String>> assets) {
        try {
            // Fetch the start_date and end_date from HoldingsMonthlyRepository
            LocalDate startDate = holdingsMonthlyRepository.findEarliestDateByAccountId(accountId);
            LocalDate endDate = holdingsMonthlyRepository.findLatestDateByAccountId(accountId);

            if (startDate == null || endDate == null) {
                logger.warn("No holdings found for accountId: {}. Skipping historical data request.", accountId);
                return;
            }
            
            // Use current date as end_date if it's later than endDate
            LocalDate currentDate = LocalDate.now();
            if (currentDate.isAfter(endDate)) {
                logger.debug("Using current date {} instead of {} as end_date", currentDate, endDate);
                endDate = currentDate;
            }
    
            // Create the payload for HISTORICAL_MARKET_DATA_REQUEST
            Map<String, Object> monthlyRequestPayload = new HashMap<>();
            monthlyRequestPayload.put("assets", assets);
            monthlyRequestPayload.put("start_date", startDate.toString());
            monthlyRequestPayload.put("end_date", endDate.toString());
            monthlyRequestPayload.put("accountId", accountId.toString());
    
            // Convert the payload to a JSON string
            String monthlyRequestJson = objectMapper.writeValueAsString(monthlyRequestPayload);
    
            // Publish the JSON payload to the HISTORICAL_MARKET_DATA_REQUEST topic
            sendKafkaMessage(KafkaTopics.HISTORICAL_MARKET_DATA_REQUEST.getTopicName(), monthlyRequestJson);
            logger.info("Sent historical market data request for accountId: {}", accountId);
        } catch (Exception e) {
            logger.error("Failed to send historical market data request: {}", e.getMessage());
        }
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
                logger.info("Fetching market data for symbol: {} assetType: {}", symbol, assetType);
                List<MarketData> marketDataList = marketDataRepository.findMarketDataBySymbolAndAssetType(symbol, assetType);
                recentMarketData.addAll(marketDataList);
            });

            if (recentMarketData.isEmpty()) {
                logger.error("No data found for symbolAssetTypePairs: {}", Arrays.deepToString(symbolAssetTypePairs.toArray()));
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
                logger.error("Retry interrupted: {}", e.getMessage());
                break;
            }

            retryCount++;
        }

        if (result.size() < symbolAssetTypePairs.size()) {
            logger.error("Failed to fetch data for all symbolAssetTypePairs after {} retries.", maxRetries);
        }

        return result;
    }

    @Override
    public KafkaTopics getUpdateRequestTopic() {
        return KafkaTopics.MARKET_DATA_UPDATE_REQUEST;
    }

    @Override
    public Map<String, Object> createUpdateRequestPayload(Map<String, Object> data) {
        // Default implementation just returns the data directly
        // Asset-specific services can override for custom payload formatting
        return data;
    }
} 