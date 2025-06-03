package com.fintrack.controller.user;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.dto.user.AccountResponse;
import com.fintrack.model.user.User;
import com.fintrack.security.JwtService;
import com.fintrack.service.user.AccountService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping(value = "/api/accounts", produces = MediaType.APPLICATION_JSON_VALUE)
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    private final AccountService accountService;
    private final JwtService jwtService;

    public AccountController(AccountService accountService, JwtService jwtService) {
        this.accountService = accountService;
        this.jwtService = jwtService;
    }

    @GetMapping(
        value = "/current",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<AccountResponse>> getCurrentAccount(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // Extract the token from the Authorization header
            String token = authorizationHeader.replace("Bearer ", "");

            // Decode the token to get the userId
            String userId = jwtService.decodeToken(token);

            // Delegate to AccountService to fetch the current account
            User user = accountService.getCurrentAccount(userId);

            // Map User to AccountResponse
            AccountResponse accountResponse = new AccountResponse();
            accountResponse.setAccountId(user.getAccountId().toString());
            accountResponse.setUserId(user.getUserId());
            accountResponse.setCreatedAt(user.getSignupDate());
            accountResponse.setUpdatedAt(user.getLastActivityDate());

            return ResponseWrapper.ok(accountResponse);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid token format: ", e);
            return ResponseWrapper.badRequest("Invalid token format");
        } catch (Exception e) {
            logger.error("Error fetching current account: ", e);
            return ResponseWrapper.internalServerError("Failed to fetch current account");
        }
    }

    @PostMapping(
        value = "/create",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> createAccountForUser(@RequestParam(name = "userId") String userId) {
        try {
            // Fetch the accountId associated with the userId from the users table
            String accountId = accountService.getAccountIdByUserId(userId);

            if (accountId == null) {
                return ResponseWrapper.notFound("User not found or account ID not associated with the user");
            }

            // Create the account in the accounts table
            accountService.createAccount(UUID.fromString(accountId));

            Map<String, String> response = Map.of(
                "message", "Account created successfully",
                "accountId", accountId
            );

            return ResponseWrapper.ok(response);
        } catch (Exception e) {
            logger.error("Error creating account: ", e);
            return ResponseWrapper.internalServerError("Failed to create account");
        }
    }
}
