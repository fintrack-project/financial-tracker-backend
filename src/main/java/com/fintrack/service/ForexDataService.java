package com.fintrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.AssetType;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.model.ForexData;
import com.fintrack.repository.ForexDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ForexDataService {

    private static final Logger logger = LoggerFactory.getLogger(ForexDataService.class);

    private final ForexDataRepository forexDataRepository;
    private final KafkaProducerService kafkaProducerService;

    public ForexDataService(ForexDataRepository forexDataRepository,
      KafkaProducerService kafkaProducerService) {
        this.forexDataRepository = forexDataRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public List<ForexData> fetchForexData(List<String> symbols) {
        // Send a Kafka message to request an update
        sendForexDataUpdateRequest(symbols);

        // Retry mechanism to fetch data until all symbols are available
        List<ForexData> result = new ArrayList<>();
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            result.clear();
            List<ForexData> recentForexData = forexDataRepository.findBySymbols(symbols);

            if (recentForexData.isEmpty()) {
                logger.error("No data found for symbols: " + symbols);
                break; // Exit if no data is found
            }

            result.addAll(recentForexData);

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

    public void sendForexDataUpdateRequest(List<String> symbols) {
        try {
            logger.info("Requesting ETL to update forex data for symbols: " + symbols);

            // Create the payload for FOREX_DATA_UPDATE_REQUEST
            Map<String, Object> updateRequestPayload = new HashMap<>();
            updateRequestPayload.put("symbols", symbols);
            updateRequestPayload.put("asset_type", AssetType.FOREX.toString());

            // Convert the payload to a JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            String updateRequestJson = objectMapper.writeValueAsString(updateRequestPayload);

            // Publish the JSON payload to the FOREX_DATA_UPDATE_REQUEST topic
            kafkaProducerService.publishEvent(KafkaTopics.FOREX_DATA_UPDATE_REQUEST.getTopicName(), updateRequestJson);
            logger.info("Sent forex data update request " + updateRequestJson);
        } catch (Exception e) {
            logger.error("Failed to send forex data update request: " + e.getMessage(), e);
            throw new RuntimeException("Failed to request ETL for forex data update", e);
        }
    }
}