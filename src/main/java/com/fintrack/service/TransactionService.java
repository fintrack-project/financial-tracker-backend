package com.fintrack.service;

import com.fintrack.model.PreviewTransaction;
import com.fintrack.model.Transaction;
import com.fintrack.repository.TransactionRepository;

import com.fintrack.constants.KafkaTopics;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.Instant;

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
    public void softDeleteByTransactionIds(List<Long> transactionIds) {
        transactionRepository.softDeleteByTransactionIds(transactionIds);
    }

    @Transactional
    public void confirmTransactions(UUID accountId, List<PreviewTransaction> previewTransactions) {
        // Separate transactions to save and delete
        List<Transaction> transactionsToSave = previewTransactions.stream()
                .filter(transaction -> !transaction.isMarkDelete())
                .map(transaction -> transaction.convertToTransaction())
                .toList();

        List<Long> transactionIdsToDelete = previewTransactions.stream()
                .filter(transaction -> transaction.isMarkDelete())
                .map(PreviewTransaction::getTransactionId)
                .toList();

        // Save new transactions
        saveAllTransactions(accountId, transactionsToSave);

        // Fetch saved transactions to get their IDs
        List<Long> transactionIdsToSave = transactionRepository.findByAccountIdOrderByDateDesc(accountId).stream()
                .filter(savedTransaction -> transactionsToSave.stream()
                        .anyMatch(toSave -> toSave.getAsset().equals(savedTransaction.getAsset())
                                && toSave.getDate().equals(savedTransaction.getDate())
                                && toSave.getCredit().equals(savedTransaction.getCredit())
                                && toSave.getDebit().equals(savedTransaction.getDebit())))
                .map(Transaction::getTransactionId)
                .toList();

        // Soft delete old transactions
        softDeleteByTransactionIds(transactionIdsToDelete);
        
        // Publish TRANSACTIONS_CONFIRMED message with only transaction IDs
        String transactionsConfirmedPayload = String.format(
                "{\"account_id\": \"%s\", \"transactions_added\": %s, \"transactions_deleted\": %s, \"timestamp\": \"%s\"}",
                accountId,
                transactionIdsToSave,
                transactionIdsToDelete,
                Instant.now().toString()
        );
        kafkaProducerService.publishEvent(KafkaTopics.TRANSACTIONS_CONFIRMED.getTopicName(), transactionsConfirmedPayload);
    }
}