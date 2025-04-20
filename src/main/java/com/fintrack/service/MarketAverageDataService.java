package com.fintrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.model.MarketAverageData;
import com.fintrack.repository.MarketAverageDataRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MarketAverageDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketAverageDataService.class);

    private final KafkaProducerService kafkaProducerService;
    private final MarketAverageDataRepository marketAverageDataRepository;

    public MarketAverageDataService(KafkaProducerService kafkaProducerService, MarketAverageDataRepository marketAverageDataRepository) {
        this.kafkaProducerService = kafkaProducerService;
        this.marketAverageDataRepository = marketAverageDataRepository;
    }

    public Map<String, Object> getMostRecentMarketAverageData(List<String> symbols) {
        // Decode all symbols first
        List<String> decodedSymbols = new ArrayList<>();
        for (String encodedSymbol : symbols) {
            decodedSymbols.add(URLDecoder.decode(encodedSymbol, StandardCharsets.UTF_8));
        }

        // Send a Kafka message to request an update
        sendMarketAverageDataUpdateRequest(decodedSymbols);

        // Retry mechanism to fetch data until all symbols are available
        Map<String, Object> result = new HashMap<>();
        int maxRetries = 5;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            result.clear();
            List<MarketAverageData> recentMarketAverageData = marketAverageDataRepository.findMarketAverageDataBySymbols(decodedSymbols);
            if(recentMarketAverageData.isEmpty()) {
                logger.error("No data found for symbols: " + decodedSymbols);
                break; // Exit if no data is found
            }

            for (MarketAverageData data : recentMarketAverageData) {
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
                Thread.sleep(2000); // Wait for 2 seconds before retrying
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

    public void sendMarketAverageDataUpdateRequest(List<String> symbols) {
        try {
            // Create the payload as a Map
            Map<String, Object> payload = new HashMap<>();
            payload.put("symbols", symbols);

            // Convert the payload to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Publish the JSON payload to the Kafka topic
            kafkaProducerService.publishEvent(KafkaTopics.MARKET_AVERAGE_DATA_UPDATE_REQUEST.getTopicName(), jsonPayload);
            logger.info("Sent market average data update request: " + jsonPayload);
        } catch (Exception e) {
            logger.error("Failed to send market average data update request: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_AVERAGE_DATA_UPDATE_COMPLETE.getTopicName()}", groupId = "market-average-data-group")
    public void onMarketAverageDataUpdateComplete(String message) {
        logger.info("Received " + KafkaTopics.MARKET_AVERAGE_DATA_UPDATE_COMPLETE.getTopicName() + " message: " + message);

        // No need to save the data; just log the message
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> indexDataList = objectMapper.readValue(message, List.class);
            for (Map<String, Object> indexData : indexDataList) {
                logger.trace("MarketAverageData: " + indexData);
            }
        } catch (Exception e) {
            logger.error("Failed to process market average data update complete message: " + e.getMessage());
        }
    }
}
