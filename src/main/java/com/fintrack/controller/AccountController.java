package com.fintrack.controller;

import com.fintrack.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, String>> getCurrentAccountId() {
        // Simulate fetching the logged-in user's ID (replace with actual authentication logic)
        String userId = "loggedInUser123"; // Replace with logic to get the logged-in user's ID

        // Fetch the accountId associated with the userId
        String accountId = accountService.getAccountIdByUserId(userId);

        if (accountId == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Account ID not found"));
        }

        return ResponseEntity.ok(Map.of("accountId", accountId));
    }
}