package com.fintrack.service;

import org.springframework.stereotype.Service;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;

@Service
public class MarketDataUpdateService {

    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    public MarketDataUpdateService(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = new ObjectMapper();
    }

    public void sendMarketDataUpdateRequest(List<String> assetNames) {
        try {
            // Create the payload as a Map
            Map<String, Object> payload = new HashMap<>();
            payload.put("asset_names", assetNames);

            // Convert the payload to a JSON string
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Publish the JSON payload to the Kafka topic
            kafkaProducerService.publishEvent(KafkaTopics.MARKET_DATA_UPDATE_REQUEST.getTopicName(), jsonPayload);
            System.out.println("Sent market data update request: " + jsonPayload);
        } catch (Exception e) {
            System.err.println("Failed to send market data update request: " + e.getMessage());
        }
    }
}