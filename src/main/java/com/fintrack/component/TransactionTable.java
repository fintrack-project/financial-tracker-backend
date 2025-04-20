package com.fintrack.component;

import java.util.*;
import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.time.LocalDate;

public class TransactionTable<T extends OverviewTransaction> {
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
    }

    public void fillTotalBalances() {
        if (isTotalBalancesFilled) {
            return; // Already filled
        }

        // Temporarily sort transactions in ascending order of date for calculation
        transactions.sort(Comparator.comparing(OverviewTransaction::getDate));

        for (T transaction : transactions) {
            String assetName = transaction.getAssetName();
            BigDecimal balanceAfter = totalBalanceAfterMap.getOrDefault(assetName, BigDecimal.ZERO);
            BigDecimal balanceBefore = initialTotalBalanceBeforeMap.getOrDefault(assetName, BigDecimal.ZERO);

            transaction.setTotalBalanceAfter(balanceAfter);
            transaction.setTotalBalanceBefore(balanceBefore);

            // Update the maps
            totalBalanceAfterMap.put(assetName, balanceAfter.add(transaction.getCredit()).subtract(transaction.getDebit()));
            initialTotalBalanceBeforeMap.put(assetName, balanceBefore.add(transaction.getCredit()).subtract(transaction.getDebit()));
        }
        isTotalBalancesFilled = true;

        // Restore the original order of transactions
        sortTransactions();
    }

    public void clear() {
        transactions.clear();
        uniqueAssetNames.clear();
        isTotalBalancesFilled = false;
        totalBalanceAfterMap.clear();
        initialTotalBalanceBeforeMap.clear();
    }
}
