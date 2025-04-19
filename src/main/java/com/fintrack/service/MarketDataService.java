package com.fintrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.model.MarketData;
import com.fintrack.repository.MarketDataRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    private final MarketDataRepository marketDataRepository;
    private final KafkaProducerService kafkaProducerService;

    public MarketDataService(MarketDataRepository marketDataRepository, KafkaProducerService kafkaProducerService) {
        this.marketDataRepository = marketDataRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public List<MarketData> fetchMarketData(List<String> symbols) {
        // Send a Kafka message to request an update
        sendMarketDataUpdateRequest(symbols);

        // Retry mechanism to fetch data until all symbols are available
        List<MarketData> result = new ArrayList<>();
        int maxRetries = 5;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            result.clear();
            List<MarketData> recentMarketData = marketDataRepository.findMarketDataBySymbols(symbols);

            if (recentMarketData.isEmpty()) {
                logger.info("No data found for symbols: " + symbols);
                break; // Exit if no data is found
            }

            result.addAll(recentMarketData);

            // Check if all symbols have data
            if (result.size() == symbols.size()) {
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

        if (result.size() < symbols.size()) {
            logger.error("Failed to fetch data for all symbols after " + maxRetries + " retries.");
        }

        return result;
    }

    public void sendMarketDataUpdateRequest(List<String> symbols) {
        try {
            // Create the payload as a Map
            Map<String, Object> payload = new HashMap<>();
            payload.put("symbols", symbols);

            // Convert the payload to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Publish the JSON payload to the Kafka topic
            kafkaProducerService.publishEvent(KafkaTopics.MARKET_DATA_UPDATE_REQUEST.getTopicName(), jsonPayload);
            logger.info("Sent market data update request: " + jsonPayload);
        } catch (Exception e) {
            logger.error("Failed to send market data update request: " + e.getMessage());
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