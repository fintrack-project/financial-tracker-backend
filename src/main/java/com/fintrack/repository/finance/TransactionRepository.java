package com.fintrack.repository.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fintrack.model.finance.Transaction;

import java.util.UUID;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Fetch transactions by accountId, excluding soft-deleted ones
    @Query(value = "SELECT * FROM transactions WHERE account_id = :accountId AND deleted_at IS NULL ORDER BY date DESC", nativeQuery = true)
    List<Transaction> findByAccountIdOrderByDateDesc(@Param("accountId") UUID accountId);

    @Query(value = "SELECT * FROM transactions WHERE account_id = :accountId AND deleted_at IS NULL AND date < :date ORDER BY date DESC", nativeQuery = true)
    List<Transaction> findByAccountIdAndDateBefore(@Param("accountId") UUID accountId, @Param("date") LocalDate date);

    // Soft delete transactions by setting the deleted_at column
    @Modifying
    @Query(value = "UPDATE transactions SET deleted_at = CURRENT_TIMESTAMP WHERE transaction_id IN (:transactionIds)", nativeQuery = true)
    void softDeleteByTransactionIds(@Param("transactionIds") List<Long> transactionIds);
}