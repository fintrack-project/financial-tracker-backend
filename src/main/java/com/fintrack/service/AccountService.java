package com.fintrack.service;

import com.fintrack.model.Account;
import com.fintrack.model.User;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(
        AccountRepository accountRepository,
        UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public User getCurrentAccount(String userId) {
        // Fetch the user by userId
        Optional<User> userOptional = userRepository.findByUserId(userId);
        if (userOptional.isPresent()) {
            return userOptional.get();
        } else {
            throw new IllegalArgumentException("User not found");
        }
    }

    public String getAccountIdByUserId(String userId) {
        // Fetch the accountId from the database using the userId
        Optional<UUID> accountId = accountRepository.findAccountIdByUserId(userId);
        return accountId.map(UUID::toString).orElse(null);
    }

    @Transactional
    public boolean createAccount(UUID accountId) {

        // Check if the account already exists
        if (accountRepository.existsById(accountId)) {
            return false; // Account already exists
        }

        // Create a new account and save it to the database
        Account account = new Account();
        account.setAccountId(accountId);
        accountRepository.save(account);

        return true; // Account creation successful
    }
}