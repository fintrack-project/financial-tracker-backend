package com.fintrack.controller;

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

    @PostMapping("/api/accounts/{accountId}/transactions")
    public ResponseEntity<String> uploadTransactions(
        @PathVariable UUID accountId,
        @RequestBody List<Transaction> transactions) {
        try {
            for (Transaction transaction : transactions) {
                transaction.setAccountId(accountId);
                transactionService.saveTransaction(transaction);
            }
            return ResponseEntity.ok("Transactions uploaded successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload transactions.");
        }
    }

    @PostMapping("/{accountId}/upload-preview-transactions")
    public ResponseEntity<List<Transaction>> uploadPreviewTransactions(
            @RequestBody List<Transaction> transactions,
            HttpSession session) {
        session.setAttribute("previewTransactions", transactions); // Store transactions in session
        return ResponseEntity.ok(transactions); // Return the stored transactions
    }

    @GetMapping("/{accountId}/upload-preview-transactions")
    public ResponseEntity<List<Transaction>> getPreviewTransactions(HttpSession session) {
        List<Transaction> previewTransactions = (List<Transaction>) session.getAttribute("previewTransactions");
        if (previewTransactions == null) {
            previewTransactions = new ArrayList<>(); // Return an empty list if no transactions are stored
        }
        return ResponseEntity.ok(previewTransactions);
    }

    @PostMapping("/api/accounts/{accountId}/confirm-transactions")
    public ResponseEntity<Void> confirmTransactions(HttpSession session) {
        List<Transaction> previewTransactions = (List<Transaction>) session.getAttribute("previewTransactions");
        if (previewTransactions != null) {
            // Save the transactions to the database (implement your save logic here)
            // transactionService.saveAll(previewTransactions);
            session.removeAttribute("previewTransactions"); // Clear the session
        }
        return ResponseEntity.ok().build();
    }
}