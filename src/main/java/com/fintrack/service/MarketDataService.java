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

    public List<MarketData> getMostRecentMarketData(List<String> symbols) {
        // Fetch the most recent market data for the given asset names
        return marketDataRepository.findMarketDataBySymbols(symbols);
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
            System.out.println("Sent market data update request: " + jsonPayload);
        } catch (Exception e) {
            System.err.println("Failed to send market data update request: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}", groupId = "market-data-group")
    public void onMarketDataUpdateComplete(String message) {
        System.out.println("Received market data update complete message: " + message);

        // No need to save the data; just log the message
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            System.out.println("MarketDataUpdate: " + payload);
        } catch (Exception e) {
            System.err.println("Failed to process market data update complete message: " + e.getMessage());
        }
    }
}