package com.financialtracker.service;

import com.financialtracker.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public String getAccountIdByUserId(String userId) {
        // Fetch the accountId from the database using the userId
        return accountRepository.findAccountIdByUserId(userId);
    }
}