package com.fintrack.repository;

import com.fintrack.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    @Query(value = "SELECT transaction_id, account_id, date, asset_name, symbol, credit, debit, unit FROM transactions WHERE account_id = :accountId ORDER BY date DESC", nativeQuery = true)
    List<Transaction> findByAccountIdOrderByDateDesc(@Param("accountId") UUID accountId);

    @Modifying
    @Query(value = "DELETE FROM transactions WHERE transaction_id IN (:transactionIds)", nativeQuery = true)
    void deleteByTransactionIds(@Param("transactionIds") List<Long> transactionIds);
}