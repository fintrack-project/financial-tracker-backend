package com.fintrack.controller;

import com.fintrack.component.PreviewTransaction;
import com.fintrack.model.Transaction;
import com.fintrack.component.OverviewTransaction;
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

    @GetMapping("/{accountId}/overview-transactions")
    public ResponseEntity<List<OverviewTransaction>> getOverviewTransactionsByAccountId(@PathVariable UUID accountId) {
        List<OverviewTransaction> transactions = transactionService.getOverviewTransactionsByAccountId(accountId);
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
    public ResponseEntity<Void> confirmTransactions(@PathVariable UUID accountId, 
    @RequestBody List<PreviewTransaction> previewTransactions,
    HttpSession session) {

        // Ensure assets exist before confirming transactions
        transactionService.ensureAssetsExist(accountId, previewTransactions);

        // Confirm transactions
        transactionService.confirmTransactions(accountId, previewTransactions);

        // Clear the session after confirmation
        session.removeAttribute("previewTransactions_" + accountId);
    
        return ResponseEntity.ok().build();
    }
}