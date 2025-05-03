package com.fintrack.controller;

import com.fintrack.service.AccountService;
import com.fintrack.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import com.fintrack.model.User; // Import the User class from the appropriate package

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final JwtService jwtService;

    public AccountController(AccountService accountService, JwtService jwtService) {
        this.accountService = accountService;
        this.jwtService = jwtService;
    }

    @GetMapping("/current")
    public ResponseEntity<User> getCurrentAccount(@RequestHeader("Authorization") String authorizationHeader) {
        // Extract the token from the Authorization header
        String token = authorizationHeader.replace("Bearer ", "");

        // Decode the token to get the userId
        String userId = jwtService.decodeToken(token);

        // Delegate to AccountService to fetch the current account
        User user = accountService.getCurrentAccount(userId);

        // Return the user details
        return ResponseEntity.ok(user);
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createAccountForUser(@RequestParam(name = "userId") String userId) {
        // Fetch the accountId associated with the userId from the users table
        String accountId = accountService.getAccountIdByUserId(userId);

        if (accountId == null) {
            return ResponseEntity.status(404)
            .body(Map.of("error", "User not found or account ID not associated with the user"));
        }

        // Create the account in the accounts table
        boolean accountCreated = accountService.createAccount(UUID.fromString(accountId));

        if (!accountCreated) {
            return ResponseEntity.status(400)
            .body(Map.of("error", "Account already exists or could not be created"));
        }

        return ResponseEntity.ok()
        .body(Map.of("message", "Account created successfully", "accountId", accountId));
    }
}