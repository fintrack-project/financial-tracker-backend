package com.fintrack.service.finance;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.model.finance.Holdings;
import com.fintrack.repository.finance.HoldingsRepository;
import com.fintrack.model.finance.Transaction;
import com.fintrack.repository.finance.TransactionRepository;

import java.util.*;

// TODO: Implement TransactionsRepository with findByAccountId(UUID)
// TODO: Ensure Transaction model has getAssetSymbol() and getQuantity() methods
// TODO: Ensure HoldingsRepository has deleteByAccountId(UUID)
// TODO: Ensure Holdings model has setAssetSymbol(String) and setQuantity(Double)

@Service
public class HoldingsService {

    private static final Logger logger = LoggerFactory.getLogger(HoldingsService.class);
    private final HoldingsRepository holdingsRepository;
    private final TransactionRepository transactionRepository;

    public HoldingsService(HoldingsRepository holdingsRepository, TransactionRepository transactionRepository) {
        this.holdingsRepository = holdingsRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Holdings> getHoldingsByAccount(UUID accountId) {
        return holdingsRepository.findHoldingsByAccount(accountId);
    }

    /**
     * Recalculate and update holdings for the given account based on all transactions.
     */
    public void updateHoldingsForAccount(UUID accountId) {
        // Fetch all transactions for the account
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByDateDesc(accountId);
        // Aggregate by assetName (unique per account)
        Map<String, Holdings> assetNameToHoldings = new HashMap<>();
        for (Transaction tx : transactions) {
            String assetName = tx.getAssetName();
            String symbol = tx.getSymbol();
            String unit = tx.getUnit();
            double netQty = tx.getCredit().subtract(tx.getDebit()).doubleValue();
            if (!assetNameToHoldings.containsKey(assetName)) {
                Holdings holding = new Holdings();
                holding.setAccountId(accountId);
                holding.setAssetName(assetName);
                holding.setSymbol(symbol);
                holding.setUnit(unit);
                holding.setTotalBalance(0.0);
                holding.setUpdatedAt(java.time.LocalDateTime.now());
                holding.setAssetType(tx.getAssetType());
                assetNameToHoldings.put(assetName, holding);
            }
            Holdings holding = assetNameToHoldings.get(assetName);
            holding.setTotalBalance(holding.getTotalBalance() + netQty);
        }
        // Remove existing holdings for the account
        holdingsRepository.deleteByAccountId(accountId);
        // Write new holdings
        for (Holdings holding : assetNameToHoldings.values()) {
            holdingsRepository.save(holding);
        }
    }

    @KafkaListener(topics = "#{T(com.fintrack.constants.KafkaTopics).PROCESS_TRANSACTIONS_TO_HOLDINGS_COMPLETE.getTopicName()}", groupId = "holdings-group")
    @CacheEvict(value = "holdings", allEntries = true)
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
