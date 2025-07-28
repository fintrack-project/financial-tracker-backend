package com.fintrack.service.finance;

import com.fintrack.repository.finance.AssetRepository;
import com.fintrack.repository.finance.HoldingsMonthlyRepository;
import com.fintrack.repository.finance.TransactionRepository;
import com.fintrack.model.finance.Asset;
import com.fintrack.model.finance.HoldingsMonthly;
import com.fintrack.model.finance.Transaction;
import com.fintrack.model.finance.Holdings;
import com.fintrack.component.transaction.OverviewTransaction;
import com.fintrack.component.transaction.PreviewTransaction;
import com.fintrack.component.transaction.TransactionTable;
import com.fintrack.constants.finance.AssetType;
import com.fintrack.service.finance.HoldingsService;
import com.fintrack.service.finance.HoldingsMonthlyService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AssetRepository assetRepository;
    private final HoldingsMonthlyRepository holdingsMonthlyRepository;
    private final HoldingsService holdingsService;
    private final HoldingsMonthlyService holdingsMonthlyService;

    public TransactionService(TransactionRepository transactionRepository, 
        AssetRepository assetRepository,
        HoldingsMonthlyRepository holdingsMonthlyRepository,
        HoldingsService holdingsService,
        HoldingsMonthlyService holdingsMonthlyService) {
        this.transactionRepository = transactionRepository;
        this.assetRepository = assetRepository;
        this.holdingsMonthlyRepository = holdingsMonthlyRepository;
        this.holdingsService = holdingsService;
        this.holdingsMonthlyService = holdingsMonthlyService;
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

        // Step 5: Calculate starting balances by subtracting the displayed transactions from current holdings
        Map<String, BigDecimal> initialTotalBalanceBeforeMap = new HashMap<>();
        
        // Get current holdings (not monthly holdings)
        List<Holdings> currentHoldings = holdingsService.getHoldingsByAccount(accountId);
        logger.debug("Current holdings: {}", currentHoldings.stream()
                .collect(Collectors.toMap(Holdings::getAssetName, Holdings::getTotalBalance)));
        
        // Start with current holdings balances
        for (Holdings holding : currentHoldings) {
            initialTotalBalanceBeforeMap.put(holding.getAssetName(), BigDecimal.valueOf(holding.getTotalBalance()));
        }
        
        // Handle missing asset names by assuming their balance is 0
        for (String assetName : uniqueAssetNames) {
            initialTotalBalanceBeforeMap.putIfAbsent(assetName, BigDecimal.ZERO);
        }
        
        // Subtract the effect of displayed transactions to get the starting balance
        // Process in reverse chronological order (newest first) to work backwards from current holdings
        logger.debug("Current holdings before subtraction: {}", initialTotalBalanceBeforeMap);
        logger.debug("Number of transactions to process: {}", transactions.size());
        
        // Create a copy of transactions and sort in reverse chronological order
        List<Transaction> transactionsForSubtraction = new ArrayList<>(transactions);
        transactionsForSubtraction.sort(Comparator.comparing(Transaction::getDate).reversed());
        
        logger.debug("Transactions sorted in reverse chronological order:");
        for (Transaction tx : transactionsForSubtraction) {
            logger.debug("  {} - {}: Credit={}, Debit={}", tx.getDate(), tx.getAssetName(), tx.getCredit(), tx.getDebit());
        }
        
        for (Transaction transaction : transactionsForSubtraction) {
            String assetName = transaction.getAssetName();
            BigDecimal currentBalance = initialTotalBalanceBeforeMap.get(assetName);
            BigDecimal transactionEffect = transaction.getCredit().subtract(transaction.getDebit());
            BigDecimal startingBalance = currentBalance.subtract(transactionEffect);
            
            logger.debug("Processing: Asset={}, Date={}, Credit={}, Debit={}, Effect={}, CurrentBalance={}, StartingBalance={}", 
                        assetName, transaction.getDate(), transaction.getCredit(), transaction.getDebit(), transactionEffect, currentBalance, startingBalance);
            
            initialTotalBalanceBeforeMap.put(assetName, startingBalance);
        }
        
        logger.debug("Final calculated starting balances: {}", initialTotalBalanceBeforeMap);

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
        logger.info("Saving {} transactions for account: {}", transactions.size(), accountId);
        
        for (Transaction transaction : transactions) {
            transaction.setAccountId(accountId); // Associate the account ID
            transactionRepository.save(transaction);
        }
        
        // Note: Holdings will be updated by the calling method (confirmTransactions)
        // to ensure proper order: delete -> add -> recalculate holdings
    }

    @Transactional
    public void softDeleteByTransactionIds(List<Long> transactionIds) {
        // Find affected account IDs before deletion
        List<Transaction> transactions = transactionRepository.findAllById(transactionIds);
        Set<UUID> affectedAccountIds = transactions.stream().map(Transaction::getAccountId).collect(Collectors.toSet());
        
        // Execute the soft delete in batches to avoid database limits
        int batchSize = 100; // Process 100 transactions at a time
        int totalUpdated = 0;
        
        for (int i = 0; i < transactionIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, transactionIds.size());
            List<Long> batch = transactionIds.subList(i, endIndex);
            
            int updatedCount = transactionRepository.softDeleteByTransactionIds(batch);
            totalUpdated += updatedCount;
        }
        
        logger.info("Soft deleted {} transactions with IDs: {}", transactionIds.size(), transactionIds);
        
        // Note: Holdings will be updated by the calling method (confirmTransactions)
        // to ensure proper order: delete -> add -> recalculate holdings
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
        logger.info("confirmTransactions called for account: {} with {} preview transactions", accountId, previewTransactions.size());
        
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

        // Step 1: Delete transactions first
        if (!transactionIdsToDelete.isEmpty()) {
            logger.info("Deleting {} transactions for account: {}", transactionIdsToDelete.size(), accountId);
            softDeleteByTransactionIds(transactionIdsToDelete);
        }

        // Step 2: Add new transactions
        if (!transactionsToSave.isEmpty()) {
            logger.info("Adding {} new transactions for account: {}", transactionsToSave.size(), accountId);
            saveAllTransactions(accountId, transactionsToSave);
        }

        // Step 3: Recalculate holdings once at the end (after all transactions are processed)
        logger.info("Recalculating holdings for account: {}", accountId);
        holdingsService.updateHoldingsForAccount(accountId);
        holdingsMonthlyService.updateMonthlyHoldingsForAccount(accountId);
    }
}