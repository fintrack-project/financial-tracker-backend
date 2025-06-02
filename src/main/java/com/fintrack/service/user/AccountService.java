package com.fintrack.service.user;

import com.fintrack.model.user.Account;
import com.fintrack.model.user.User;
import com.fintrack.repository.user.AccountRepository;
import com.fintrack.repository.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        // Fetch the user by userId
        Optional<User> userOptional = userRepository.findByUserId(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return user.getAccountId() != null ? user.getAccountId().toString() : null;
        }
        return null;
    }

    @Transactional
    public void createAccount(UUID accountId) {
        // Check if account already exists
        if (accountRepository.existsById(accountId)) {
            return;
        }

        // Create new account
        Account account = new Account();
        account.setAccountId(accountId);
        account.setCreatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }
}