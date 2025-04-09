package com.fintrack.service;

import com.fintrack.model.Transaction;
import com.fintrack.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getTransactionsByAccountId(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByDateDesc(accountId);
    }

    public void saveTransaction(UUID accountId, Transaction transaction) {
        transaction.setAccountId(accountId);
        transactionRepository.save(transaction);
    }

    public void saveAllTransactions(UUID accountId, List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            transaction.setAccountId(accountId); // Associate the account ID
            transactionRepository.save(transaction);
        }
    }

    public void deleteByTransactionId(Long transactionId) {
        transactionRepository.deleteByTransactionId(transactionId);
    }
}