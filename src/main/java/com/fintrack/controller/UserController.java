package com.fintrack.controller;

import com.fintrack.model.User;
import com.fintrack.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginUser(@RequestBody Map<String, String> loginRequest) {
        String userId = loginRequest.get("userId");
        String password = loginRequest.get("password");
    
        // Authenticate the user and generate a JWT token
        Map<String, Object> response = userService.authenticateAndGenerateToken(userId, password);
    
        if ((boolean) response.get("success")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        String result = userService.registerUser(user);

        if (result.equals("User registered successfully.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/fetch")
    public ResponseEntity<?> fetchUserDetails(@RequestBody Map<String, String> requestBody) {
        String accountIdStr = requestBody.get("accountId");
        if (accountIdStr == null) {
            return ResponseEntity.badRequest().body("Missing accountId in the request body.");
        }
    
        try {
            UUID accountId = UUID.fromString(accountIdStr);
            Optional<User> user = userService.fetchUserDetails(accountId);
    
            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                return ResponseEntity.status(404).body("User not found.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid accountId format.");
        }
    }

    @PostMapping("/update-phone")
    public ResponseEntity<Void> updateUserPhone(@RequestBody Map<String, Object> request) {
        String accountIdString = (String) request.get("accountId");
        String phone = (String) request.get("phone");
        String countryCode = (String) request.get("countryCode");

        UUID accountId = UUID.fromString(accountIdString);
        userService.updateUserPhone(accountId, phone, countryCode);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-address")
    public ResponseEntity<Void> updateUserAddress(@RequestBody Map<String, Object> request) {
        String accountIdString = (String) request.get("accountId");
        String address = (String) request.get("address");

        UUID accountId = UUID.fromString(accountIdString);
        userService.updateUserAddress(accountId, address);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-email")
    public ResponseEntity<Void> updateUserEmail(@RequestBody Map<String, Object> request) {
        String accountIdString = (String) request.get("accountId");
        String email = (String) request.get("email");

        UUID accountId = UUID.fromString(accountIdString);
        userService.updateUserEmail(accountId, email);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-2fa")
    public ResponseEntity<Void> updateTwoFactorStatus(@RequestBody Map<String, Object> request) {
        String accountIdString = (String) request.get("accountId");
        Boolean enabled = (Boolean) request.get("enabled");
    
        if (accountIdString == null || enabled == null) {
            return ResponseEntity.badRequest().build(); // Return 400 if required fields are missing
        }
    
        try {
            UUID accountId = UUID.fromString(accountIdString);
            userService.updateTwoFactorStatus(accountId, enabled);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); // Return 400 if accountId is invalid
        }
    }
}