package com.fintrack.controller.finance;

import com.fintrack.component.transaction.OverviewTransaction;
import com.fintrack.component.transaction.PreviewTransaction;
import com.fintrack.model.finance.Transaction;
import com.fintrack.service.finance.TransactionService;
import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping(value = "/api/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{accountId}/transactions")
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactionsByAccountId(@PathVariable UUID accountId) {
        try {
            List<Transaction> transactions = transactionService.getTransactionsByAccountId(accountId);
            return ResponseWrapper.ok(transactions);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @GetMapping("/{accountId}/overview-transactions")
    public ResponseEntity<ApiResponse<List<OverviewTransaction>>> getOverviewTransactionsByAccountId(
            @PathVariable UUID accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            List<OverviewTransaction> transactions;
            
            if (startDate != null && endDate != null) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                transactions = transactionService.getOverviewTransactionsByAccountIdAndDateRange(accountId, start, end);
            } else {
                transactions = transactionService.getOverviewTransactionsByAccountId(accountId);
            }
            
            return ResponseWrapper.ok(transactions);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("/{accountId}/upload-preview-transactions")
    public ResponseEntity<ApiResponse<List<Transaction>>> uploadPreviewTransactions(
            @RequestBody List<Transaction> transactions,
            @PathVariable UUID accountId,
            HttpSession session) {
        try {
            logger.info("Processing uploaded preview transactions for account ID: {}", accountId);
        
            // Delegate the logic to transactionService to process and set the unit
            List<Transaction> processedTransactions = transactionService.processPreviewTransactions(transactions);
        
            // Store the processed transactions in the session
            session.setAttribute("previewTransactions", processedTransactions);
        
            // Return the processed transactions
            return ResponseWrapper.ok(processedTransactions);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @GetMapping("/{accountId}/preview-transactions")
    public ResponseEntity<ApiResponse<List<Transaction>>> getPreviewTransactions(HttpSession session) {
        try {
            logger.info("Fetching preview transactions from session");
            logger.trace("Session ID: {}", session.getId());
            logger.trace("Session Attributes: {}", session.getAttributeNames());

            List<Transaction> previewTransactions = (List<Transaction>) session.getAttribute("previewTransactions");
            if (previewTransactions == null) {
                previewTransactions = new ArrayList<>(); // Return an empty list if no transactions are stored
            }
            return ResponseWrapper.ok(previewTransactions);
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }

    @PostMapping("{accountId}/confirm-transactions")
    public ResponseEntity<ApiResponse<Void>> confirmTransactions(@PathVariable UUID accountId, 
    @RequestBody List<PreviewTransaction> previewTransactions,
    HttpSession session) {
        try {
            logger.info("Confirming transactions for account ID: {}", accountId);
            logger.trace("Session ID: {}", session.getId());
            logger.trace("Session Attributes: {}", session.getAttributeNames());

            // Ensure assets exist before confirming transactions
            transactionService.ensureAssetsExist(accountId, previewTransactions);

            // Confirm transactions
            transactionService.confirmTransactions(accountId, previewTransactions);

            // Clear the session after confirmation
            session.removeAttribute("previewTransactions_" + accountId);
        
            return ResponseWrapper.ok(null, "Transactions confirmed successfully.");
        } catch (Exception e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
}