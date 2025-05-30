package com.fintrack.controller.user;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.model.finance.AccountCurrency;
import com.fintrack.repository.finance.AccountCurrenciesRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping(value = "/api/currencies", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountCurrenciesController {

    private static final Logger logger = LoggerFactory.getLogger(AccountCurrenciesController.class);
    private final AccountCurrenciesRepository accountCurrenciesRepository;

    public AccountCurrenciesController(AccountCurrenciesRepository accountCurrenciesRepository) {
        this.accountCurrenciesRepository = accountCurrenciesRepository;
    }

    @GetMapping(
        value = "/fetch",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<List<AccountCurrency>>> getCurrenciesByAccountId(@RequestParam(name = "accountId") UUID accountId) {
        try {
            List<AccountCurrency> currencies = accountCurrenciesRepository.findByAccountId(accountId);
            return ResponseWrapper.ok(currencies);
        } catch (Exception e) {
            logger.error("Error fetching currencies: ", e);
            return ResponseWrapper.internalServerError("Failed to fetch currencies");
        }
    }

    @PostMapping(
        value = "/set-base-currency",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Void>> updateBaseCurrency(@RequestBody Map<String, Object> request) {
        try {
            UUID accountId = UUID.fromString((String) request.get("accountId"));
            String baseCurrency = (String) request.get("baseCurrency");

            if (accountId == null || baseCurrency == null) {
                return ResponseWrapper.badRequest("Missing accountId or baseCurrency");
            }

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

            return ResponseWrapper.ok(null, "Base currency updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest("Invalid accountId format");
        } catch (Exception e) {
            logger.error("Error updating base currency: ", e);
            return ResponseWrapper.internalServerError("Failed to update base currency");
        }
    }
}