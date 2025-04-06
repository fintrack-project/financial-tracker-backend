package com.fintrack.controller;

import com.fintrack.model.Transaction;
import com.fintrack.service.TransactionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
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
}