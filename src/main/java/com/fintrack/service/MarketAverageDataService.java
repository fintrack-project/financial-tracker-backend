package com.fintrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.model.MarketAverageData;
import com.fintrack.repository.MarketAverageDataRepository;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MarketAverageDataService {

    private final KafkaProducerService kafkaProducerService;
    private final MarketAverageDataRepository marketAverageDataRepository;

    public MarketAverageDataService(KafkaProducerService kafkaProducerService, MarketAverageDataRepository marketAverageDataRepository) {
        this.kafkaProducerService = kafkaProducerService;
        this.marketAverageDataRepository = marketAverageDataRepository;
    }

    public Map<String, Object> getMostRecentMarketData(List<String> symbols) {
        // Send a Kafka message to request an update
        sendMarketAverageDataUpdateRequest(symbols);

        // Retry mechanism to fetch data until all symbols are available
        Map<String, Object> result = new HashMap<>();
        int maxRetries = 5;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            result.clear();
            for (String encodedSymbol : symbols) {
                String symbol = URLDecoder.decode(encodedSymbol, StandardCharsets.UTF_8);
                MarketAverageData mostRecentData = marketAverageDataRepository.findTopBySymbolOrderByTimeDesc(symbol);
                if (mostRecentData != null) {
                    result.put(symbol, Map.of(
                        "price", mostRecentData.getPrice(),
                        "price_change", mostRecentData.getPriceChange(),
                        "percent_change", mostRecentData.getPercentChange(),
                        "price_low", mostRecentData.getPriceLow(),
                        "price_high", mostRecentData.getPriceHigh()
                    ));
                }
            }

            // Check if all symbols have data
            if (result.size() == symbols.size()) {
                break;
            }

            // Wait before retrying
            try {
                Thread.sleep(2000); // Wait for 2 seconds before retrying
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Retry interrupted: " + e.getMessage());
                break;
            }

            retryCount++;
        }

        if (result.size() < symbols.size()) {
            System.err.println("Failed to fetch data for all symbols after " + maxRetries + " retries.");
        }

        return result;
    }

    public void sendMarketAverageDataUpdateRequest(List<String> indexNames) {
        try {
            // Create the payload as a Map
            Map<String, Object> payload = new HashMap<>();
            payload.put("index_names", indexNames);

            // Convert the payload to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonPayload = objectMapper.writeValueAsString(payload);

            // Publish the JSON payload to the Kafka topic
            kafkaProducerService.publishEvent(KafkaTopics.MARKET_AVERAGE_DATA_UPDATE_REQUEST.getTopicName(), jsonPayload);
            System.out.println("Sent market average data update request: " + jsonPayload);
        } catch (Exception e) {
            System.err.println("Failed to send market average data update request: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).MARKET_AVERAGE_DATA_UPDATE_COMPLETE.getTopicName()}", groupId = "market-average-data-group")
    public void onMarketAverageDataUpdateComplete(String message) {
        System.out.println("Received market average data update complete message: " + message);

        if (message == null || message.trim().isEmpty() || message.equals("{}")) {
            System.err.println("Received an empty or invalid message. Skipping processing.");
            return;
        }

        try {
            // Parse the JSON message
            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> indexDataList = objectMapper.readValue(message, List.class);

            // Save the updated data to the database
            for (Map<String, Object> indexData : indexDataList) {
                MarketAverageData marketAverageData = new MarketAverageData();
                marketAverageData.setSymbol((String) indexData.get("symbol"));
                marketAverageData.setPrice(indexData.get("price").toString());
                marketAverageData.setPriceChange((Double) indexData.get("price_change"));
                marketAverageData.setPercentChange(indexData.get("percent_change").toString());
                marketAverageData.setPriceHigh((Double) indexData.get("price_high"));
                marketAverageData.setPriceLow((Double) indexData.get("price_low"));
                marketAverageData.setTime(LocalDateTime.now()); // Set the current timestamp

                System.out.println("MarketAverageData: " + marketAverageData);
            }
        } catch (Exception e) {
            System.err.println("Failed to process market average data update complete message: " + e.getMessage());
        }
    }
}
