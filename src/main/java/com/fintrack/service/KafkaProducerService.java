package com.fintrack.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEvent(String topic, String message) {
        kafkaTemplate.send(topic, message)
            .thenAccept(result -> System.out.println("Message sent successfully to topic: " + topic))
            .exceptionally(ex -> {
                System.err.println("Failed to send message to topic: " + topic + " - " + ex.getMessage());
                return null;
            });
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