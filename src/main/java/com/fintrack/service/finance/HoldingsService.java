package com.fintrack.service.finance;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

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
        logger.info("Starting holdings recalculation for account: {}", accountId);
        
        // Fetch all transactions for the account
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByDateDesc(accountId);
        logger.info("Found {} transactions for account: {}", transactions.size(), accountId);
        
        // Debug: Log each transaction
        for (Transaction tx : transactions) {
            logger.info("Transaction: ID={}, Asset={}, Credit={}, Debit={}, Date={}, DeletedAt={}", 
                       tx.getTransactionId(), tx.getAssetName(), tx.getCredit(), tx.getDebit(), 
                       tx.getDate(), tx.getDeletedAt());
        }
        
        // If no transactions, just delete all holdings and return
        if (transactions.isEmpty()) {
            logger.info("No transactions found, deleting all holdings for account: {}", accountId);
            holdingsRepository.deleteByAccountId(accountId);
            return;
        }
        
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
        
        logger.info("Calculated holdings for {} assets: {}", assetNameToHoldings.size(), 
                   assetNameToHoldings.keySet());
        
        // Debug: Log calculated balances
        for (Holdings holding : assetNameToHoldings.values()) {
            logger.info("Calculated holding: Asset={}, Balance={}", 
                       holding.getAssetName(), holding.getTotalBalance());
        }
        
        // Since we delete transactions first, holdings table should be empty
        // But we'll still delete to be safe
        logger.info("Deleting existing holdings for account: {}", accountId);
        int deletedCount = holdingsRepository.deleteByAccountId(accountId);
        logger.info("Deleted {} existing holdings for account: {}", deletedCount, accountId);
        
        // Save all holdings (since we delete first, table is empty)
        List<Holdings> holdingsToSave = new ArrayList<>(assetNameToHoldings.values());
        logger.info("Saving {} holdings for account: {}", holdingsToSave.size(), accountId);
        
        if (!holdingsToSave.isEmpty()) {
            holdingsRepository.saveAll(holdingsToSave);
        }
        
        logger.info("Completed holdings recalculation for account: {}", accountId);
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

