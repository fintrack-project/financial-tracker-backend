package com.fintrack.service.finance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fintrack.model.finance.Holdings;
import com.fintrack.repository.finance.HoldingsRepository;
import com.fintrack.model.finance.Transaction;
import com.fintrack.repository.finance.TransactionRepository;
import com.fintrack.constants.finance.AssetType;

import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.math.BigDecimal;

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
        
        for (Transaction transaction : transactions) {
            String assetName = transaction.getAssetName();
            String symbol = transaction.getSymbol();
            String unit = transaction.getUnit();
            AssetType assetType = transaction.getAssetType();
            
            // Calculate net quantity (credit - debit)
            BigDecimal netQty = transaction.getCredit().subtract(transaction.getDebit());
            
            // Get or create holdings for this asset
            Holdings holdings = assetNameToHoldings.computeIfAbsent(assetName, k -> {
                Holdings newHoldings = new Holdings();
                newHoldings.setAccountId(accountId);
                newHoldings.setAssetName(assetName);
                newHoldings.setSymbol(symbol);
                newHoldings.setUnit(unit);
                newHoldings.setAssetType(assetType);
                newHoldings.setTotalBalance(0.0);
                newHoldings.setUpdatedAt(LocalDateTime.now());
                return newHoldings;
            });
            
            // Add net quantity to total balance
            holdings.setTotalBalance(holdings.getTotalBalance() + netQty.doubleValue());
        }
        
        // Remove holdings with zero or negative balance
        List<Holdings> holdingsToSave = assetNameToHoldings.values().stream()
            .filter(holdings -> holdings.getTotalBalance() > 0)
            .collect(Collectors.toList());
        
        // Delete existing holdings for this account
        holdingsRepository.deleteByAccountId(accountId);
        
        logger.info("Saving {} holdings for account: {}", holdingsToSave.size(), accountId);
        
        if (!holdingsToSave.isEmpty()) {
            holdingsRepository.saveAll(holdingsToSave);
        }
        
        logger.info("Completed holdings recalculation for account: {}", accountId);
    }
}

