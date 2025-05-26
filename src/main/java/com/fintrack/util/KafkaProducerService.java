package com.fintrack.util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Primary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

public interface KafkaProducerService {
    void publishEvent(String topic, String message);
    void publishEventWithRetry(String topic, String message, int maxRetries, long retryIntervalMillis);
    void publishEventsAtomically(List<Map.Entry<String, String>> events);
}

@Service
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers", matchIfMissing = false)
class KafkaProducerServiceImpl implements KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerServiceImpl.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerServiceImpl(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishEvent(String topic, String message) {
        publishEventWithRetry(topic, message, 3, 500);
    }

    @Override
    public void publishEventWithRetry(String topic, String message, int maxRetries, long retryIntervalMillis) {
        AtomicInteger attempt = new AtomicInteger(0);
        boolean success = false;
    
        while (attempt.get() < maxRetries && !success) {
            try {
                attempt.incrementAndGet();
                CompletableFuture<Void> future = kafkaTemplate.send(topic, message)
                        .thenAccept(result -> {
                            logger.info("Message sent successfully to topic: " + topic + ", partition: " + result.getRecordMetadata().partition() + ", offset: " + result.getRecordMetadata().offset());
                        })
                        .exceptionally(ex -> {
                            logger.error("Failed to send message to topic: " + topic + ", error: " + ex.getMessage());
                            return null;
                        });
    
                future.get(10, TimeUnit.SECONDS);
                success = true;
            } catch (Exception e) {
                logger.error("Attempt {}: Exception occurred while sending message to topic: {}, error: {}", attempt.get(), topic, e.getMessage());
                if (attempt.get() < maxRetries) {
                    try {
                        Thread.sleep(retryIntervalMillis);
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

    @Override
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

@Service
@Primary
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers", havingValue = "", matchIfMissing = true)
class NoOpKafkaProducerService implements KafkaProducerService {
    private static final Logger logger = LoggerFactory.getLogger(NoOpKafkaProducerService.class);

    public NoOpKafkaProducerService() {
        logger.info("Using NoOpKafkaProducerService - Kafka is not configured");
    }

    @Override
    public void publishEvent(String topic, String message) {
        logger.debug("Kafka not configured - message not sent to topic: {}", topic);
    }

    @Override
    public void publishEventWithRetry(String topic, String message, int maxRetries, long retryIntervalMillis) {
        logger.debug("Kafka not configured - message not sent to topic: {}", topic);
    }

    @Override
    public void publishEventsAtomically(List<Map.Entry<String, String>> events) {
        logger.debug("Kafka not configured - messages not sent");
    }
}