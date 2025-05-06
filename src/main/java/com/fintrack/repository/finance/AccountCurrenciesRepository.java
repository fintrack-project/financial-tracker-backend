package com.fintrack.repository.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fintrack.model.finance.AccountCurrency;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

@Repository
public interface AccountCurrenciesRepository extends JpaRepository<AccountCurrency, Long> {

    @Query(value = "SELECT * FROM account_currencies WHERE account_id = :accountId", nativeQuery = true)
    List<AccountCurrency> findByAccountId(@Param("accountId") UUID accountId);

    Optional<AccountCurrency> findByAccountIdAndIsDefault(UUID accountId, boolean isDefault);

    @Query(value = "SELECT * FROM account_currencies WHERE account_id = :accountId AND currency = :currency", nativeQuery = true)
    Optional<AccountCurrency> findByAccountIdAndCurrency(@Param("accountId") UUID accountId, @Param("currency") String currency);
}