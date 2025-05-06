package com.fintrack.controller.user;

import com.fintrack.model.finance.AccountCurrency;
import com.fintrack.repository.finance.AccountCurrenciesRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/currencies")
public class AccountCurrenciesController {

    private final AccountCurrenciesRepository accountCurrenciesRepository;

    public AccountCurrenciesController(AccountCurrenciesRepository accountCurrenciesRepository) {
        this.accountCurrenciesRepository = accountCurrenciesRepository;
    }

    @GetMapping("/fetch")
    public ResponseEntity<List<AccountCurrency>> getCurrenciesByAccountId(@RequestParam(name = "accountId") UUID accountId) {
        List<AccountCurrency> currencies = accountCurrenciesRepository.findByAccountId(accountId);
        return ResponseEntity.ok(currencies);
    }

    @PostMapping("/set-base-currency")
    public ResponseEntity<Void> updateBaseCurrency(@RequestBody Map<String, Object> request) {
        UUID accountId = UUID.fromString((String) request.get("accountId"));
        String baseCurrency = (String) request.get("baseCurrency");

        // Find the existing default currency and unset it
        accountCurrenciesRepository.findByAccountIdAndIsDefault(accountId, true)
                .ifPresent(existingDefault -> {
                    existingDefault.setDefault(false);
                    accountCurrenciesRepository.save(existingDefault);
                });

        // Find or create the new base currency and set it as default
        Optional<AccountCurrency> baseCurrencyOptional = accountCurrenciesRepository.findByAccountIdAndCurrency(accountId, baseCurrency);
        if (baseCurrencyOptional.isPresent()) {
            AccountCurrency baseCurrencyEntity = baseCurrencyOptional.get();
            baseCurrencyEntity.setDefault(true);
            accountCurrenciesRepository.save(baseCurrencyEntity);
        } else {
            AccountCurrency newBaseCurrency = new AccountCurrency(accountId, baseCurrency, true);
            accountCurrenciesRepository.save(newBaseCurrency);
        }

        return ResponseEntity.ok().build();
    }
}