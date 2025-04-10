package com.fintrack.controller;

import com.fintrack.model.PreviewTransaction;
import com.fintrack.model.Transaction;
import com.fintrack.service.TransactionService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<List<Transaction>> getTransactionsByAccountId(@PathVariable UUID accountId) {
        List<Transaction> transactions = transactionService.getTransactionsByAccountId(accountId);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/{accountId}/upload-preview-transactions")
    public ResponseEntity<List<Transaction>> uploadPreviewTransactions(
            @RequestBody List<Transaction> transactions,
            HttpSession session) {
        session.setAttribute("previewTransactions", transactions); // Store transactions in session
        return ResponseEntity.ok(transactions); // Return the stored transactions
    }

    @GetMapping("/{accountId}/preview-transactions")
    public ResponseEntity<List<Transaction>> getPreviewTransactions(HttpSession session) {
        List<Transaction> previewTransactions = (List<Transaction>) session.getAttribute("previewTransactions");
        if (previewTransactions == null) {
            previewTransactions = new ArrayList<>(); // Return an empty list if no transactions are stored
        }
        return ResponseEntity.ok(previewTransactions);
    }

    @PostMapping("{accountId}/confirm-transactions")
    public ResponseEntity<List<Transaction>> confirmTransactions(@PathVariable UUID accountId, 
    @RequestBody List<PreviewTransaction> previewTransactions,
    HttpSession session) {
        // Separate transactions to save and delete
        List<Transaction> transactionsToSave = previewTransactions.stream()
                .filter(transaction -> !transaction.isMarkDelete()) // Keep only transactions not marked for deletion
                .map(this::convertToTransaction) // Convert PreviewTransaction to Transaction
                .toList();

        List<Transaction> transactionsToDelete = previewTransactions.stream()
                .filter(transaction -> transaction.getTransactionId() != null) // exclude transactions by upload
                .filter(transaction -> transaction.isMarkDelete()) // Keep only transactions marked for deletion
                .map(this::convertToTransaction) // Convert PreviewTransaction to Transaction
                .toList();

        // Save transactions to the database
        transactionService.saveAllTransactions(accountId, transactionsToSave);

        // Delete transactions from the database
        for(Transaction transactionToDelete : transactionsToDelete){
            transactionService.deleteByTransactionId(transactionToDelete.getTransactionId());
        }
        
        // Update the session with only the transactions not marked for deletion
        session.setAttribute("previewTransactions_" + accountId, transactionsToSave);

        return ResponseEntity.ok(transactionsToSave);
    }

    // Helper method to convert PreviewTransaction to Transaction
    private Transaction convertToTransaction(PreviewTransaction previewTransaction) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(previewTransaction.getTransactionId());
        transaction.setAccountId(previewTransaction.getAccountId());
        transaction.setDate(previewTransaction.getDate());
        transaction.setAssetName(previewTransaction.getAssetName());
        transaction.setCredit(previewTransaction.getCredit());
        transaction.setDebit(previewTransaction.getDebit());
        transaction.setTotalBalanceBefore(previewTransaction.getTotalBalanceBefore());
        transaction.setTotalBalanceAfter(previewTransaction.getTotalBalanceAfter());
        transaction.setUnit(previewTransaction.getUnit());
        return transaction;
    }
}