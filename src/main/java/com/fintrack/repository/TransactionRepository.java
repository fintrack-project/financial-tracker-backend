package com.fintrack.repository;

import com.fintrack.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query(value = "SELECT transaction_id, account_id, date, asset_name, credit, debit, total_balance_before, total_balance_after, unit FROM transactions WHERE account_id = :accountId ORDER BY date ASC", nativeQuery = true)
    List<Transaction> findByAccountIdOrderByDateAsc(@Param("accountId") UUID accountId);
}