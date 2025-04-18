package com.fintrack.model;

public class PreviewTransaction extends Transaction {
    private boolean markDelete;

    public Transaction convertToTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(getTransactionId());
        transaction.setAccountId(getAccountId());
        transaction.setDate(getDate());
        transaction.setAsset(getAsset());
        transaction.setCredit(getCredit());
        transaction.setDebit(getDebit());
        transaction.setDeletedAt(getDeletedAt());
        return transaction;
    }

    public boolean isMarkDelete() {
        return markDelete;
    }

    public void setMarkDelete(boolean markDelete) {
        this.markDelete = markDelete;
    }
}