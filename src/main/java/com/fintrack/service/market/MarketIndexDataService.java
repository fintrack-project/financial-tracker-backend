package com.fintrack.service.market;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.model.market.MarketIndexData;
import com.fintrack.repository.market.MarketIndexDataRepository;
import com.fintrack.service.market.base.AbstractMarketDataProvider;
import com.fintrack.util.KafkaProducerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MarketIndexDataService extends AbstractMarketDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(MarketIndexDataService.class);

    private final MarketIndexDataRepository marketIndexDataRepository;

    public MarketIndexDataService(KafkaProducerService kafkaProducerService, MarketIndexDataRepository marketIndexDataRepository) {
        super(kafkaProducerService);
        this.marketIndexDataRepository = marketIndexDataRepository;
    }

    public Map<String, Object> getMostRecentMarketIndexData(List<String> symbols) {
        // Decode all symbols first
        List<String> decodedSymbols = new ArrayList<>();
        for (String encodedSymbol : symbols) {
            decodedSymbols.add(URLDecoder.decode(encodedSymbol, StandardCharsets.UTF_8));
        }

        // Request an update via the superclass method
        requestMarketDataUpdate(decodedSymbols);

        // Retry mechanism to fetch data until all symbols are available
        Map<String, Object> result = new HashMap<>();
        int maxRetries = 3;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            result.clear();
            List<MarketIndexData> recentMarketIndexData = marketIndexDataRepository.findMarketIndexDataBySymbols(decodedSymbols);
            if(recentMarketIndexData.isEmpty()) {
                logger.error("No data found for symbols: " + decodedSymbols);
                break; // Exit if no data is found
            }

            for (MarketIndexData data : recentMarketIndexData) {
                result.put(data.getSymbol(), Map.of(
                    "price", data.getPrice(),
                    "price_change", data.getPriceChange(),
                    "percent_change", data.getPercentChange(),
                    "price_low", data.getPriceLow(),
                    "price_high", data.getPriceHigh()
                ));
            }

            // Check if all symbols have data
            if (result.size() == decodedSymbols.size()) {
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

        if (result.size() < decodedSymbols.size()) {
            logger.error("Failed to fetch data for all symbols after " + maxRetries + " retries.");
        }

        return result;
    }

    @Override
    public KafkaTopics getUpdateRequestTopic() {
        return KafkaTopics.MARKET_INDEX_DATA_UPDATE_REQUEST;
    }

    @Override
    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_INDEX_DATA_UPDATE_COMPLETE.getTopicName()}", groupId = "market-index-data-group")
    public void onMarketDataUpdateComplete(String message) {
        logger.info("Received " + KafkaTopics.MARKET_INDEX_DATA_UPDATE_COMPLETE.getTopicName() + " message: " + message);

        // No need to save the data; just log the message
        try {
            List<Map<String, Object>> indexDataList = objectMapper.readValue(message, List.class);
            for (Map<String, Object> indexData : indexDataList) {
                logger.trace("MarketIndexData: " + indexData);
            }
        } catch (Exception e) {
            logger.error("Failed to process market index data update complete message: " + e.getMessage());
        }
    }
}
