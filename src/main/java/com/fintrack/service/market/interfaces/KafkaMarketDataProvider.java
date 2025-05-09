package com.fintrack.service.market.interfaces;

import com.fintrack.constants.KafkaTopics;

import java.util.Map;

/**
 * Interface for market data providers that use Kafka for sending update requests.
 * This interface extends the basic MarketDataProvider with Kafka-specific functionality.
 */
public interface KafkaMarketDataProvider extends MarketDataProvider {
    
    /**
     * Sends a market data update request message to Kafka.
     * 
     * @param topic The Kafka topic to send the message to
     * @param payload The payload to send
     */
    void sendKafkaMessage(String topic, String payload);
    
    /**
     * Handles market data update completion messages from Kafka.
     * This method will typically be annotated with @KafkaListener in implementations.
     * 
     * @param message The received Kafka message
     */
    void onMarketDataUpdateComplete(String message);
    
    /**
     * Gets the Kafka topic for market data update requests.
     * 
     * @return The Kafka topic name
     */
    KafkaTopics getUpdateRequestTopic();
    
    /**
     * Creates a standard market data update request payload.
     * 
     * @param data The data to include in the payload
     * @return The formatted payload as a map
     */
    Map<String, Object> createUpdateRequestPayload(Map<String, Object> data);
} 