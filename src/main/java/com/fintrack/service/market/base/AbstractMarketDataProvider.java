package com.fintrack.service.market.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.service.market.interfaces.KafkaMarketDataProvider;
import com.fintrack.util.KafkaProducerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract base class implementing common functionality for market data providers
 * that use Kafka for sending update requests.
 */
public abstract class AbstractMarketDataProvider implements KafkaMarketDataProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final KafkaProducerService kafkaProducerService;
    protected final ObjectMapper objectMapper;
    
    /**
     * Constructor for AbstractMarketDataProvider.
     * 
     * @param kafkaProducerService The Kafka producer service to use for sending messages
     */
    public AbstractMarketDataProvider(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public void requestMarketDataUpdate(List<String> symbols) {
        try {
            // Create payload for Kafka message
            Map<String, Object> payload = createUpdateRequestPayload(Map.of("symbols", symbols));
            
            // Convert to JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            // Send Kafka message
            sendKafkaMessage(getUpdateRequestTopic().getTopicName(), jsonPayload);
            
            logger.info("Requested market data update for {} symbols", symbols.size());
        } catch (Exception e) {
            logger.error("Failed to request market data update: {}", e.getMessage());
        }
    }
    
    @Override
    public void requestMarketDataUpdate(UUID accountId, List<String> symbols) {
        try {
            // Create payload for Kafka message with accountId
            Map<String, Object> data = new HashMap<>();
            data.put("symbols", symbols);
            data.put("accountId", accountId.toString());
            
            Map<String, Object> payload = createUpdateRequestPayload(data);
            
            // Convert to JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);
            
            // Send Kafka message
            sendKafkaMessage(getUpdateRequestTopic().getTopicName(), jsonPayload);
            
            logger.info("Requested market data update for account {} with {} symbols", accountId, symbols.size());
        } catch (Exception e) {
            logger.error("Failed to request market data update for account {}: {}", accountId, e.getMessage());
        }
    }
    
    @Override
    public void sendKafkaMessage(String topic, String payload) {
        try {
            kafkaProducerService.publishEvent(topic, payload);
            logger.debug("Sent message to Kafka topic {}: {}", topic, payload);
        } catch (Exception e) {
            logger.error("Failed to send Kafka message to topic {}: {}", topic, e.getMessage());
            throw e; // Re-throw to allow method-specific handling
        }
    }
    
    @Override
    public Map<String, Object> createUpdateRequestPayload(Map<String, Object> data) {
        // Default implementation just returns the data directly
        // Subclasses can override for custom payload formatting
        return data;
    }
} 