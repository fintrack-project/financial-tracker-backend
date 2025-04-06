package com.fintrack.controller;

import com.fintrack.service.AccountService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, String>> getCurrentAccountId(HttpServletRequest request) {
        // Retrieve the userId from the session (or token)
        String userId = (String) request.getSession().getAttribute("userId"); // Example: Retrieve userId from session

        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not logged in"));
        }

        // Fetch the accountId associated with the userId
        String accountId = accountService.getAccountIdByUserId(userId);

        if (accountId == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Account ID not found"));
        }

        return ResponseEntity.ok(Map.of("accountId", accountId));
    }
}