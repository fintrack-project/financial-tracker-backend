package com.fintrack.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.model.HoldingDto;
import com.fintrack.repository.HoldingsRepository;

import java.util.*;

@Service
public class HoldingsService {
    private HoldingsRepository holdingsRepository;

    public HoldingsService(HoldingsRepository holdingsRepository) {
        this.holdingsRepository = holdingsRepository;
    }

    public List<HoldingDto> getLatestHoldingsByAccount(UUID accountId) {
        return holdingsRepository.findLatestHoldingsByAccount(accountId);
    }

    @KafkaListener(topics = "PROCESS_TRANSACTIONS_TO_HOLDINGS", groupId = "holdings-group")
    public void processTransactionsToHoldings(String message) {
        // Parse the message
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            UUID accountId = UUID.fromString((String) payload.get("account_id"));
            System.out.println("Received PROCESS_TRANSACTIONS_TO_HOLDINGS for account: " + accountId);

            // Fetch the latest holdings
            List<HoldingDto> holdings = getLatestHoldingsByAccount(accountId);
            System.out.println("Latest holdings: " + holdings);
        } catch (Exception e) {
            System.err.println("Failed to process message: " + e.getMessage());
        }
    }
}
