package com.fintrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.model.MarketData;
import com.fintrack.repository.MarketDataRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MarketDataService {

    private final MarketDataRepository marketDataRepository;
    private final KafkaProducerService kafkaProducerService;

    public MarketDataService(MarketDataRepository marketDataRepository, KafkaProducerService kafkaProducerService) {
        this.marketDataRepository = marketDataRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public List<MarketData> fetchMarketDataByAssetNames(List<String> assetNames) {
        // Fetch market data for the specified asset names
        return marketDataRepository.findMarketDataByAssetNames(assetNames);
    }

    public void sendMarketDataUpdateRequest(List<String> assetNames) {
        try {
            // Create the payload as a Map
            Map<String, Object> payload = new HashMap<>();
            payload.put("asset_names", assetNames);

            // Create an ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();

            // Convert the payload to a JSON string
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Publish the JSON payload to the Kafka topic
            kafkaProducerService.publishEvent(KafkaTopics.MARKET_DATA_UPDATE_REQUEST.getTopicName(), jsonPayload);
            System.out.println("Sent market data update request: " + jsonPayload);
        } catch (Exception e) {
            System.err.println("Failed to send market data update request: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}", groupId = "market-data-group")
    public void onMarketDataUpdateComplete(String message) {
        System.out.println("Received market data update complete message: " + message);

        // Create an ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Parse the JSON message
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);

            // Extract the assets and status
            List<String> assets = (List<String>) payload.get("assets");
            String status = (String) payload.get("status");

            if ("complete".equalsIgnoreCase(status) && assets != null && !assets.isEmpty()) {
                // Send the updated market data to the WebSocket topic
                marketDataRepository.findMarketDataByAssetNames(assets);
            } else {
                System.err.println("Invalid or incomplete message: " + message);
            }
        } catch (Exception e) {
            System.err.println("Failed to process market data update complete message: " + e.getMessage());
        }
    }
}