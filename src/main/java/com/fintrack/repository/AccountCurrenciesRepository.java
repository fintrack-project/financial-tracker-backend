package com.fintrack.repository;

import com.fintrack.model.AccountCurrency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountCurrenciesRepository extends JpaRepository<AccountCurrency, Long> {

    @Query(value = "SELECT * FROM account_currencies WHERE account_id = :accountId AND currency = :currency", nativeQuery = true)
    Optional<AccountCurrency> findByAccountIdAndCurrency(@Param("accountId") UUID accountId, @Param("currency") String currency);
}