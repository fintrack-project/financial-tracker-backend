package com.fintrack.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.controller.MarketDataWebSocketController;

import java.util.*;

@Service
public class MarketDataUpdateListener {

    private final MarketDataWebSocketController marketDataWebSocketController;
    private final ObjectMapper objectMapper;

    public MarketDataUpdateListener(MarketDataWebSocketController marketDataWebSocketController) {
        this.marketDataWebSocketController = marketDataWebSocketController;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_DATA_UPDATE_COMPLETE.getTopicName()}", groupId = "market-data-group")
    public void onMarketDataUpdateComplete(String message) {
        System.out.println("Received market data update complete message: " + message);

        try {
            // Parse the JSON message
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);

            // Extract the assets and status
            List<String> assets = (List<String>) payload.get("assets");
            String status = (String) payload.get("status");

            if ("complete".equalsIgnoreCase(status) && assets != null && !assets.isEmpty()) {
                // Send the updated market data to the WebSocket topic
                marketDataWebSocketController.sendUpdatedMarketData(assets);
            } else {
                System.err.println("Invalid or incomplete message: " + message);
            }
        } catch (Exception e) {
            System.err.println("Failed to process market data update complete message: " + e.getMessage());
        }
    }
}