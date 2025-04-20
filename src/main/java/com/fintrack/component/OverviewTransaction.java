package com.fintrack.component;

import java.math.BigDecimal;

import com.fintrack.model.Transaction;

public class OverviewTransaction extends Transaction {
    private BigDecimal totalBalanceBefore = BigDecimal.ZERO;
    private BigDecimal totalBalanceAfter = BigDecimal.ZERO;

    public OverviewTransaction() {
        super();
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
