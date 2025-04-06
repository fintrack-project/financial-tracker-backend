package com.fintrack.repository;

import com.fintrack.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query(value = "SELECT date, asset_name, credit, debit, total_balance_before, total_balance_after, unit FROM transactions WHERE account_id = :accountId", nativeQuery = true)
    List<Transaction> findByAccountIdOrderByDateAsc(String accountId);
}