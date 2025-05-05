package com.fintrack.controller;

import com.fintrack.model.UserSubscription;
import com.fintrack.service.UserSubscriptionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/user/subscriptions")
public class UserSubscriptionController {

    private final UserSubscriptionService userSubscriptionService;

    public UserSubscriptionController(UserSubscriptionService userSubscriptionService) {
        this.userSubscriptionService = userSubscriptionService;
    }

    @PostMapping("/fetch")
    public ResponseEntity<?> fetchUserSubscriptionDetails(@RequestBody Map<String, String> requestBody) {
        String accountIdStr = requestBody.get("accountId");
        if (accountIdStr == null) {
            return ResponseEntity.badRequest().body("Missing accountId in the request body.");
        }
    
        try {
            UUID accountId = UUID.fromString(accountIdStr);
            Optional<UserSubscription> user = userSubscriptionService.getSubscriptionByAccountId(accountId);
    
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(404).body("User not found.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid accountId format.");
        }
    }
}