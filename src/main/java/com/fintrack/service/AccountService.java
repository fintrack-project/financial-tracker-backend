package com.fintrack.service;

import com.fintrack.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String getAccountIdByUserId(String userId) {
        // Fetch the accountId from the database using the userId
        Optional<UUID> accountId = accountRepository.findAccountIdByUserId(userId);
        return accountId.map(UUID::toString).orElse(null);
    }
}