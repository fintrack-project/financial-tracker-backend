package com.fintrack.service;

import com.fintrack.model.Transaction;
import com.fintrack.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> getTransactionsByAccountId(String accountId) {
        return transactionRepository.findByAccountIdOrderByDateAsc(accountId);
    }

    public void saveTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
    }
}