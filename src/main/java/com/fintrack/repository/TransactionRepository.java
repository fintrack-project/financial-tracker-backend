package com.fintrack.repository;

import com.fintrack.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // Fetch transactions by accountId, excluding soft-deleted ones
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId AND t.deletedAt IS NULL ORDER BY t.date DESC")
    List<Transaction> findByAccountIdOrderByDateDesc(@Param("accountId") UUID accountId);

    List<Transaction> findByAccountIdAndDateBefore(UUID accountId, LocalDate date);

    // Soft delete transactions by setting the deleted_at column
    @Modifying
    @Query("UPDATE Transaction t SET t.deletedAt = CURRENT_TIMESTAMP WHERE t.transactionId IN :transactionIds")
    void softDeleteByTransactionIds(@Param("transactionIds") List<Long> transactionIds);
}