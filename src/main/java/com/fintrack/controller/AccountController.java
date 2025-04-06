package com.fintrack.controller;

import com.fintrack.service.AccountService;
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
    public ResponseEntity<Map<String, UUID>> getCurrentAccountId(@RequestParam String userId) {
        // Fetch the accountId associated with the userId
        UUID accountId = accountService.getAccountIdByUserId(userId);

        if (accountId == null) {
          return ResponseEntity.status(404).body(Collections.singletonMap("error", null));
      }

      return ResponseEntity.ok(Collections.singletonMap("accountId", accountId));
    }
}