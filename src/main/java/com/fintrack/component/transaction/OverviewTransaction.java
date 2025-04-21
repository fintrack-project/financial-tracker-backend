package com.fintrack.component.transaction;

import java.math.BigDecimal;

import com.fintrack.model.Transaction;

public class OverviewTransaction extends Transaction {
    private BigDecimal totalBalanceBefore;
    private BigDecimal totalBalanceAfter;

    public OverviewTransaction() {
        super();
    }

    public OverviewTransaction(Transaction transaction) {
        super();
        setTransactionId(transaction.getTransactionId());
        setAccountId(transaction.getAccountId());
        setDate(transaction.getDate());
        setAsset(transaction.getAsset());
        setAssetName(transaction.getAssetName());
        setSymbol(transaction.getSymbol());
        setUnit(transaction.getUnit());
        setCredit(transaction.getCredit());
        setDebit(transaction.getDebit());
        setDeletedAt(transaction.getDeletedAt());
    }

    public Transaction convertToTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(getTransactionId());
        transaction.setAccountId(getAccountId());
        transaction.setDate(getDate());
        transaction.setAsset(getAsset());
        transaction.setAssetName(getAssetName());
        transaction.setSymbol(getSymbol());
        transaction.setUnit(getUnit());
        transaction.setCredit(getCredit());
        transaction.setDebit(getDebit());
        transaction.setDeletedAt(getDeletedAt());
        return transaction;
    }

    public BigDecimal getTotalBalanceBefore() {
        return totalBalanceBefore;
    }

    public void setTotalBalanceBefore(BigDecimal totalBalanceBefore) {
        this.totalBalanceBefore = totalBalanceBefore;
    }

    public BigDecimal getTotalBalanceAfter() {
        return totalBalanceAfter;
    }

    public void setTotalBalanceAfter(BigDecimal totalBalanceAfter) {
        this.totalBalanceAfter = totalBalanceAfter;
    }
}
