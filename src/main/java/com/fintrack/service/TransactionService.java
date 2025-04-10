package com.fintrack.service;

import com.fintrack.model.PreviewTransaction;
import com.fintrack.model.Transaction;
import com.fintrack.repository.TransactionRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.time.Instant;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final KafkaProducerService kafkaProducerService;

    public TransactionService(TransactionRepository transactionRepository, 
        KafkaProducerService kafkaProducerService) {
        this.transactionRepository = transactionRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    public List<Transaction> getTransactionsByAccountId(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByDateDesc(accountId);
    }

    @Transactional
    public void saveAllTransactions(UUID accountId, List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            transaction.setAccountId(accountId); // Associate the account ID
            transactionRepository.save(transaction);
        }
    }

    @Transactional
    public void deleteByTransactionIds(List<Long> transactionIds) {
        transactionRepository.deleteByTransactionIds(transactionIds);
    }

    @Transactional
    public void confirmTransactions(UUID accountId, List<PreviewTransaction> previewTransactions) {
        // Separate transactions to save and delete
        List<Transaction> transactionsToSave = previewTransactions.stream()
                .filter(transaction -> !transaction.isMarkDelete())
                .map(this::convertToTransaction)
                .toList();

        List<Long> transactionIdsToDelete = previewTransactions.stream()
                .filter(transaction -> transaction.isMarkDelete())
                .map(PreviewTransaction::getTransactionId)
                .toList();

        // Save new transactions
        saveAllTransactions(accountId, transactionsToSave);

        // Delete old transactions
        deleteByTransactionIds(transactionIdsToDelete);
        
        // Publish a single TRANSACTIONS_CONFIRMED event
        String eventPayload = String.format(
            "{\"account_id\": \"%s\", \"transactions_added\": %s, \"transactions_deleted\": %s, \"timestamp\": \"%s\"}",
            accountId,
            transactionsToSave,
            transactionIdsToDelete,
            Instant.now().toString()
        );
        kafkaProducerService.publishEvent("TRANSACTIONS_CONFIRMED", eventPayload);
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