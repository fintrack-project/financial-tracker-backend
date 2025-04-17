package com.fintrack.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.model.HoldingDto;
import com.fintrack.repository.HoldingsRepository;

import java.util.*;

@Service
public class HoldingsService {
    private HoldingsRepository holdingsRepository;

    public HoldingsService(HoldingsRepository holdingsRepository) {
        this.holdingsRepository = holdingsRepository;
    }

    public List<HoldingDto> getHoldingsByAccount(UUID accountId) {
        return holdingsRepository.findHoldingsByAccount(accountId);
    }

    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).PROCESS_TRANSACTIONS_TO_HOLDINGS.getTopicName()}", groupId = "holdings-group")
    public void processTransactionsToHoldings(String message) {
        // Parse the message
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            UUID accountId = UUID.fromString((String) payload.get("account_id"));
            System.out.println("Received " + KafkaTopics.PROCESS_TRANSACTIONS_TO_HOLDINGS.getTopicName() + " for account: " + accountId);

            // Fetch the latest holdings
            List<HoldingDto> holdings = getHoldingsByAccount(accountId);
            System.out.println("Latest holdings: " + holdings);
        } catch (Exception e) {
            System.err.println("Failed to process message: " + e.getMessage());
        }
    }
}
