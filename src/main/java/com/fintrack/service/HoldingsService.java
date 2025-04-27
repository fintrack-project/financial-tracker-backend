package com.fintrack.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.model.Holdings;
import com.fintrack.repository.HoldingsRepository;

import java.util.*;

@Service
public class HoldingsService {

    private static final Logger logger = LoggerFactory.getLogger(HoldingsService.class);

    private HoldingsRepository holdingsRepository;

    public HoldingsService(HoldingsRepository holdingsRepository) {
        this.holdingsRepository = holdingsRepository;
    }

    public List<Holdings> getHoldingsByAccount(UUID accountId) {
        return holdingsRepository.findHoldingsByAccount(accountId);
    }

    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).PROCESS_TRANSACTIONS_TO_HOLDINGS_COMPLETE.getTopicName()}", groupId = "holdings-group")
    public void processTransactionsToHoldings(String message) {
        // Parse the message
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            // Fetch the latest holdings
        } catch (Exception e) {
            logger.error("Error processing message: " + e.getMessage(), e);
        }
    }
}
