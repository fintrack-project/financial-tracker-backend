package com.fintrack.service.finance;

import com.fintrack.repository.finance.AssetRepository;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.finance.TransactionRepository;
import com.fintrack.util.KafkaProducerService;
import com.fintrack.model.finance.Asset;
import com.fintrack.model.finance.HoldingsMonthly;
import com.fintrack.model.finance.Transaction;
import com.fintrack.component.transaction.OverviewTransaction;
import com.fintrack.component.transaction.PreviewTransaction;
import com.fintrack.component.transaction.TransactionTable;
import com.fintrack.constants.KafkaTopics;
import com.fintrack.constants.finance.AssetType;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AssetRepository assetRepository;
    private final HoldingsMonthlyRepository holdingsMonthlyRepository;
    private final KafkaProducerService kafkaProducerService;

    public TransactionService(TransactionRepository transactionRepository, 
        AssetRepository assetRepository,
        HoldingsMonthlyRepository holdingsMonthlyRepository,
        KafkaProducerService kafkaProducerService) {
        this.transactionRepository = transactionRepository;
        this.assetRepository = assetRepository;
        this.holdingsMonthlyRepository = holdingsMonthlyRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionsByAccountId(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByDateDesc(accountId);
    }

    @Transactional(readOnly = true)
    public List<OverviewTransaction> getOverviewTransactionsByAccountId(UUID accountId) {
        return getOverviewTransactionsByAccountIdAndDateRange(accountId, null, null);
    }

    @Transactional(readOnly = true)
    public List<OverviewTransaction> getOverviewTransactionsByAccountIdAndDateRange(UUID accountId, LocalDate startDate, LocalDate endDate) {
        // Step 1: Get transactions by account ID in descending order of date (with optional date range filter)
        List<Transaction> transactions;
        if (startDate != null && endDate != null) {
            transactions = transactionRepository.findByAccountIdAndDateBetweenOrderByDateDesc(accountId, startDate, endDate);
        } else {
            transactions = transactionRepository.findByAccountIdOrderByDateDesc(accountId);
        }

        transactions.forEach(
            transaction -> { 
                logger.trace("transaction, account id: " + accountId + ", date : " + transaction.getDate() + ", asset name: " + transaction.getAssetName() + ", credit: " + transaction.getCredit() + ", debit: " + transaction.getDebit());
            }
        );

        // Step 2: Create a TransactionTable object
        TransactionTable<OverviewTransaction> transactionTable = new TransactionTable<>(
            transactions.stream()
                .map(transaction -> new OverviewTransaction(transaction))
                .collect(Collectors.toCollection(ArrayList::new)) // Convert to mutable list
        );

        // Step 3: Get the earliest date and unique asset names from the transactions
        Optional<LocalDate> earliestDateOptional = transactionTable.getEarliestTransactionDate();
        List<String> uniqueAssetNames = transactionTable.getUniqueAssetNames();

        if (earliestDateOptional.isEmpty()) {
            return Collections.emptyList(); // No transactions found
        }

        LocalDate earliestDate = earliestDateOptional.get();

        // Step 4: Find the 1st date of the month for the earliest date
        LocalDate firstDateOfMonth = earliestDate.withDayOfMonth(1);
        LocalDate lastDateOfMonth = firstDateOfMonth.withDayOfMonth(firstDateOfMonth.lengthOfMonth());

        // Fetch monthly holdings after the 1st date of the month
        List<HoldingsMonthly> monthlyHoldings = holdingsMonthlyRepository.findByAccountIdAndDateBetween(accountId, firstDateOfMonth, lastDateOfMonth);

        monthlyHoldings.forEach(
            holding -> { 
                logger.trace("holdingsMonthly, account id: " + accountId + ", date : " + holding.getDate() + ", asset name: " + holding.getAssetName() + ", total balance: " + holding.getTotalBalance());
            }
        );

        // Step 5: Set initialTotalBalanceBeforeMap using monthly holdings
        Map<String, BigDecimal> initialTotalBalanceBeforeMap = new HashMap<>();
        for (HoldingsMonthly holding : monthlyHoldings) {
            initialTotalBalanceBeforeMap.put(holding.getAssetName(), holding.getTotalBalance());
        }

        // Handle missing asset names by assuming their balance is 0
        for (String assetName : uniqueAssetNames) {
            initialTotalBalanceBeforeMap.putIfAbsent(assetName, BigDecimal.ZERO);
        }

        logger.trace("Initial total balance before map: " + initialTotalBalanceBeforeMap);

        // Step 6: Add transactions up to the initial date (only if no date range filter is applied)
        if (startDate == null || endDate == null) {
            List<Transaction> transactionsBeforeEarliestDate = transactionRepository.findByAccountIdAndDateBefore(accountId, earliestDate);

            logger.trace("Transactions before earliest date: " + earliestDate);
            transactionsBeforeEarliestDate.forEach(
                transaction -> { 
                    logger.trace("transaction, account id: " + accountId + ", date : " + transaction.getDate() + ", asset name: " + transaction.getAssetName() + ", credit: " + transaction.getCredit() + ", debit: " + transaction.getDebit());
                }
            );

            transactionTable.addTransactions(
                transactionsBeforeEarliestDate.stream()
                    .map(transaction -> new OverviewTransaction(transaction))
                    .collect(Collectors.toList())
            );
        }

        // Step 7: Update total balance information in the TransactionTable
        transactionTable.setInitialTotalBalanceBeforeMap(initialTotalBalanceBeforeMap);
        transactionTable.fillTotalBalances();

        // Step 8: Return the transactions directly from the TransactionTable
        return transactionTable.getTransactions();
    }

    public List<Transaction> processPreviewTransactions(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            switch (transaction.getAssetType()) {
                case STOCK:
                    transaction.setUnit("SHARE");
                    break;
                case CRYPTO:
                case FOREX:
                    transaction.setUnit(transaction.getSymbol());
                    break;
                case COMMODITY:
                    transaction.setUnit("UNIT");
                    break;
                default:
                    logger.warn("Unknown asset type for transaction: {}", transaction);
                    transaction.setUnit("UNKNOWN");
            }
        }
        return transactions;
    }

    @Transactional
    public void saveAllTransactions(UUID accountId, List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            transaction.setAccountId(accountId); // Associate the account ID
            transactionRepository.save(transaction);
        }
    }

    @Transactional
    public void softDeleteByTransactionIds(List<Long> transactionIds) {
        transactionRepository.softDeleteByTransactionIds(transactionIds);
    }

    @Transactional
    public void ensureAssetsExist(UUID accountId, List<PreviewTransaction> previewTransactions) {
        for (PreviewTransaction previewTransaction : previewTransactions) {
            String assetName = previewTransaction.getAssetName();
            String symbol = previewTransaction.getSymbol();
            String unit = previewTransaction.getUnit();
            AssetType assetType = previewTransaction.getAssetType();
    
            // Check if the Asset exists
            Optional<Asset> assetOptional = assetRepository.findByAccountIdAndAssetName(accountId, assetName);
            if (assetOptional.isEmpty()) {
                // Create a new Asset if it does not exist
                Asset asset = new Asset();
                asset.setAccountId(accountId);
                asset.setAssetName(assetName);
                asset.setSymbol(symbol);
                asset.setUnit(unit);
                asset.setAssetType(assetType);
                assetRepository.save(asset);
            }
        }
    }

    @Transactional
    public void confirmTransactions(UUID accountId, List<PreviewTransaction> previewTransactions) {
        // Separate transactions to save and delete
        List<Transaction> transactionsToSave = previewTransactions.stream()
            .filter(transaction -> !transaction.isMarkDelete()) // Exclude transactions marked for deletion
            .map(PreviewTransaction::convertToTransaction)
            .filter(newTransaction -> newTransaction.getTransactionId() == null && newTransaction.getAccountId() == null) // Only save new transactions
            .toList();

        List<Long> transactionIdsToDelete = previewTransactions.stream()
                .filter(transaction -> transaction.isMarkDelete())
                .map(PreviewTransaction::getTransactionId)
                .filter(Objects::nonNull) // Exclude unsaved transactions (those without a transactionId)
                .toList();

        // Save new transactions
        saveAllTransactions(accountId, transactionsToSave);

        // Fetch saved transactions to get their IDs
        List<Long> transactionIdsToSave = transactionRepository.findByAccountIdOrderByDateDesc(accountId).stream()
                .filter(savedTransaction -> transactionsToSave.stream()
                        .anyMatch(toSave -> toSave.getAssetName().equals(savedTransaction.getAssetName())
                                && toSave.getDate().equals(savedTransaction.getDate())
                                && toSave.getCredit().equals(savedTransaction.getCredit())
                                && toSave.getDebit().equals(savedTransaction.getDebit())
                                && toSave.getUnit().equals(savedTransaction.getUnit())
                                && Objects.equals(toSave.getSymbol(), savedTransaction.getSymbol())))
                .map(Transaction::getTransactionId)
                .toList();

        // Soft delete old transactions
        softDeleteByTransactionIds(transactionIdsToDelete);
        
        // Publish TRANSACTIONS_CONFIRMED message with only transaction IDs
        String transactionsConfirmedPayload = String.format(
                "{\"account_id\": \"%s\", \"transactions_added\": %s, \"transactions_deleted\": %s, \"timestamp\": \"%s\"}",
                accountId,
                transactionIdsToSave,
                transactionIdsToDelete,
                Instant.now().toString()
        );
        kafkaProducerService.publishEventWithRetry(
            KafkaTopics.TRANSACTIONS_CONFIRMED.getTopicName(), 
            transactionsConfirmedPayload, 
            3, 
            2000);
    }
}