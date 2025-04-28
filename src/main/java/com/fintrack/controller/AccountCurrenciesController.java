package com.fintrack.controller;

import com.fintrack.model.AccountCurrency;
import com.fintrack.repository.AccountCurrenciesRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
public class AccountCurrenciesController {

    private final AccountCurrenciesRepository accountCurrenciesRepository;

    public AccountCurrenciesController(AccountCurrenciesRepository accountCurrenciesRepository) {
        this.accountCurrenciesRepository = accountCurrenciesRepository;
    }

    @GetMapping("/{accountId}/currencies")
    public ResponseEntity<List<String>> getCurrenciesByAccountId(@PathVariable UUID accountId) {
        List<String> currencies = accountCurrenciesRepository.findByAccountId(accountId)
                .stream()
                .map(accountCurrency -> accountCurrency.getCurrency())
                .collect(Collectors.toList());
        return ResponseEntity.ok(currencies);
    }
}