package com.fintrack.controller;

import com.fintrack.model.Account;
import com.fintrack.model.User;
import com.fintrack.repository.UserRepository;
import com.fintrack.repository.AccountRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private AccountRepository accountRepository;

    public UserController(
        UserRepository userRepository, 
        BCryptPasswordEncoder passwordEncoder, 
        AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User loginRequest, HttpServletRequest request) {
        Optional<User> user = userRepository.findByUserId(loginRequest.getUserId());

        if (user.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), user.get().getPassword())) {
            // Store userId in the session
            request.getSession().setAttribute("userId", loginRequest.getUserId());
            return ResponseEntity.ok("Login successful!");
        } else {
            return ResponseEntity.status(401).body("Invalid user ID or password.");
        }
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        // Check if user ID already exists
        if (userRepository.findByUserId(user.getUserId()).isPresent()) {
            return ResponseEntity.badRequest().body("User ID already exists.");
        }

        // Check if user email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists.");
        }

        // Generate a random account_id
        UUID accountId = UUID.randomUUID();

        // Create and save the account
        Account newAccount = new Account();
        newAccount.setAccountId(accountId);
        accountRepository.saveAndFlush(newAccount);

        // Set the generated account_id in the user object
        user.setAccountId(accountId);

        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully.");
    }
}