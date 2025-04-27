package com.fintrack.controller;

import com.fintrack.component.transaction.OverviewTransaction;
import com.fintrack.component.transaction.PreviewTransaction;
import com.fintrack.model.Transaction;
import com.fintrack.service.TransactionService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/accounts")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

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
            @PathVariable UUID accountId,
            HttpSession session) {
        logger.info("Processing uploaded preview transactions for account ID: {}", accountId);
    
        // Delegate the logic to transactionService to process and set the unit
        List<Transaction> processedTransactions = transactionService.processPreviewTransactions(transactions);
    
        // Store the processed transactions in the session
        session.setAttribute("previewTransactions", processedTransactions);
    
        // Return the processed transactions
        return ResponseEntity.ok(processedTransactions);
    }

    @GetMapping("/{accountId}/preview-transactions")
    public ResponseEntity<List<Transaction>> getPreviewTransactions(HttpSession session) {
        logger.info("Fetching preview transactions from session");
        logger.trace("Session ID: {}", session.getId());
        logger.trace("Session Attributes: {}", session.getAttributeNames());

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
        logger.info("Confirming transactions for account ID: {}", accountId);
        logger.trace("Session ID: {}", session.getId());
        logger.trace("Session Attributes: {}", session.getAttributeNames());

        // Ensure assets exist before confirming transactions
        transactionService.ensureAssetsExist(accountId, previewTransactions);

        // Confirm transactions
        transactionService.confirmTransactions(accountId, previewTransactions);

        // Clear the session after confirmation
        session.removeAttribute("previewTransactions_" + accountId);
    
        return ResponseEntity.ok().build();
    }
}