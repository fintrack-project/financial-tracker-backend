package com.fintrack.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEvent(String topic, String message) {
        kafkaTemplate.send(topic, message)
            .thenAccept(result -> logger.info("Message sent successfully to topic: " + topic + ", partition: " + result.getRecordMetadata().partition() + ", offset: " + result.getRecordMetadata().offset()))
            .exceptionally(ex -> {
                logger.error("Failed to send message to topic: " + topic + ", error: " + ex.getMessage());
                return null;
            });
    }

    public void publishEventWithRetry(String topic, String message, int maxRetries, long retryIntervalMillis) {
        AtomicInteger attempt = new AtomicInteger(0); // Use AtomicInteger to make it mutable
        boolean success = false;
    
        while (attempt.get() < maxRetries && !success) {
            try {
                attempt.incrementAndGet(); // Increment the attempt count
                CompletableFuture<Void> future = kafkaTemplate.send(topic, message)
                        .thenAccept(result -> {
                            logger.info("Message sent successfully to topic: " + topic + ", partition: " + result.getRecordMetadata().partition() + ", offset: " + result.getRecordMetadata().offset());
                        })
                        .exceptionally(ex -> {
                            logger.error("Failed to send message to topic: " + topic + ", error: " + ex.getMessage());
                            return null;
                        });
    
                // Wait for the send to complete
                future.get(10, TimeUnit.SECONDS); // Timeout after 10 seconds
                success = true; // If no exception, mark as success
            } catch (Exception e) {
                System.err.printf("Attempt %d: Exception occurred while sending message to topic: %s, error: %s%n",
                        attempt.get(), topic, e.getMessage());
                if (attempt.get() < maxRetries) {
                    try {
                        Thread.sleep(retryIntervalMillis); // Wait before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                } else {
                    throw new RuntimeException("Failed to send message after " + maxRetries + " attempts", e);
                }
            }
        }
    }

    /**
     * Publishes multiple events atomically to Kafka.
     *
     * @param events A list of topic-message pairs to publish.
     */
    public void publishEventsAtomically(List<Map.Entry<String, String>> events) {
        kafkaTemplate.executeInTransaction(operations -> {
            for (Map.Entry<String, String> event : events) {
                String topic = event.getKey();
                String message = event.getValue();
                operations.send(topic, message);
            }
            return null;
        });
    }
}