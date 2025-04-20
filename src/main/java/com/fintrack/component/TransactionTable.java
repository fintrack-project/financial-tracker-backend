package com.fintrack.component;

import java.util.*;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionTable<T extends OverviewTransaction> {
    private static final Logger logger = LoggerFactory.getLogger(TransactionTable.class);

    private List<T> transactions;
    private List<String> uniqueAssetNames;
    private Boolean isTotalBalancesFilled = false;
    private Map<String, BigDecimal> totalBalanceAfterMap = new HashMap<>();
    private Map<String, BigDecimal> initialTotalBalanceBeforeMap = new HashMap<>();

    public TransactionTable(List<T> transactions) {
        this.transactions = transactions;
        this.uniqueAssetNames = transactions.stream()
                .map(OverviewTransaction::getAssetName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        sortTransactions();
    }

    public TransactionTable(List<T> transactions, Map<String, BigDecimal> initialTotalBalanceBeforeMap) {
        this.transactions = transactions;
        this.initialTotalBalanceBeforeMap = initialTotalBalanceBeforeMap;
        sortTransactions();
    }

    public List<T> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<T> transactions) {
        // Replace the existing transactions and sort
        this.transactions = new ArrayList<>(transactions);
        sortTransactions();
    }

    public Optional<LocalDate> getEarliestTransactionDate() {
        return transactions.stream()
                .map(OverviewTransaction::getDate)
                .min(LocalDate::compareTo);
    }

    public List<String> getUniqueAssetNames() {
        return uniqueAssetNames;
    }

    public void setUniqueAssetNames(List<String> uniqueAssetNames) {
        this.uniqueAssetNames = uniqueAssetNames;
    } 

    public Boolean getIsTotalBalancesFilled() {
        return isTotalBalancesFilled;
    }

    public void setIsTotalBalancesFilled(Boolean isTotalBalancesFilled) {
        this.isTotalBalancesFilled = isTotalBalancesFilled;
    }

    public Map<String, BigDecimal> getTotalBalanceAfterMap() {
        if (isTotalBalancesFilled) {
            return totalBalanceAfterMap;
        } else {
            throw new IllegalStateException("Total balances are not filled");
        }
    }

    public void setTotalBalanceAfterMap(Map<String, BigDecimal> totalBalanceAfterMap) {
        this.totalBalanceAfterMap = totalBalanceAfterMap;
    }

    public Map<String, BigDecimal> getInitialTotalBalanceBeforeMap() {
        return initialTotalBalanceBeforeMap;
    }

    public void setInitialTotalBalanceBeforeMap(Map<String, BigDecimal> initialTotalBalanceBeforeMap) {
        this.initialTotalBalanceBeforeMap = initialTotalBalanceBeforeMap;
    }

    public void addTransactions(List<T> newTransactions) {
        // Add new transactions to the existing list
        this.transactions.addAll(newTransactions);
        // Sort the combined list
        sortTransactions();
    }

    private void sortTransactions() {
        // Sort the transactions based on the below criteria
        transactions.sort(Comparator
                .comparing(OverviewTransaction::getDate, Comparator.reverseOrder()) // Descending order of date
                .thenComparing(OverviewTransaction::getAssetName) // Ascending order of asset name
                .thenComparing(OverviewTransaction::getCredit) // Ascending order of credit
                .thenComparing(OverviewTransaction::getDebit)); // Ascending order of debit

        logger.info("Transactions sorted in descending order of date, ascending order of asset name, credit, and debit.");
        transactions.forEach(transaction -> 
            logger.info("Sorted transaction, date: {}, assetName: {}, credit: {}, debit: {}",
                transaction.getDate(), transaction.getAssetName(), transaction.getCredit(), transaction.getDebit())
        );
    }

    public void fillTotalBalances() {
        if (isTotalBalancesFilled) {
            logger.warn("Total balances are already filled. No need to fill again.");
            return; // Already filled
        }

        logger.info("Starting total balance calculation...");
        logger.info("Initial totalBalanceAfterMap: {}", totalBalanceAfterMap);
        logger.info("Initial initialTotalBalanceBeforeMap: {}", initialTotalBalanceBeforeMap);

        // Deep copy initialTotalBalanceBeforeMap to totalBalanceBeforeMap
        Map<String, BigDecimal> totalBalanceBeforeMap = new HashMap<>(initialTotalBalanceBeforeMap);
        logger.info("Deep copied initialTotalBalanceBeforeMap to totalBalanceBeforeMap: {}", totalBalanceBeforeMap);

        // Temporarily sort transactions in ascending order of date for calculation
        transactions.sort(Comparator.comparing(OverviewTransaction::getDate));
        logger.info("Transactions sorted in ascending order of date for calculation.");
        transactions.forEach(transaction -> 
            logger.info("Sorted transaction, date: {}, assetName: {}, credit: {}, debit: {}",
                transaction.getDate(), transaction.getAssetName(), transaction.getCredit(), transaction.getDebit())
        );

        for (T transaction : transactions) {
            String assetName = transaction.getAssetName();
            BigDecimal balanceBefore = totalBalanceBeforeMap.getOrDefault(assetName, BigDecimal.ZERO);
            BigDecimal balanceAfter = balanceBefore.add(transaction.getCredit()).subtract(transaction.getDebit());

            logger.info("Processing transaction: {}", transaction);
            logger.info("Current balanceBefore for asset {}: {}", assetName, balanceBefore);
            logger.info("Current balanceAfter for asset {}: {}", assetName, balanceAfter);

            transaction.setTotalBalanceBefore(balanceBefore);
            transaction.setTotalBalanceAfter(balanceAfter);
            
            // Update the maps
            totalBalanceBeforeMap.put(assetName, balanceAfter);
            totalBalanceAfterMap.put(assetName, balanceAfter);

            logger.info("Updated balanceAfter for asset {}: {}", assetName, totalBalanceAfterMap.get(assetName));
            logger.info("Updated balanceBefore for asset {}: {}", assetName, totalBalanceBeforeMap.get(assetName));
 
        }
        isTotalBalancesFilled = true;

        logger.info("Total balance calculation completed.");
        logger.info("Final totalBalanceAfterMap: {}", totalBalanceAfterMap);
        logger.info("Final initialTotalBalanceBeforeMap: {}", initialTotalBalanceBeforeMap);


        // Restore the original order of transactions
        sortTransactions();
        logger.info("Transactions restored to original sorting order.");
        transactions.forEach(transaction -> 
            logger.info("Sorted transaction, date: {}, assetName: {}, credit: {}, debit: {}",
                transaction.getDate(), transaction.getAssetName(), transaction.getCredit(), transaction.getDebit())
        );
    }

    public void clear() {
        transactions.clear();
        uniqueAssetNames.clear();
        isTotalBalancesFilled = false;
        totalBalanceAfterMap.clear();
        initialTotalBalanceBeforeMap.clear();
    }
}
