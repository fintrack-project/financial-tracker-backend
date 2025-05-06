package com.fintrack.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fintrack.service.user.UserTwoFactorService;

import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserTwoFactorController {

    private final UserTwoFactorService userTwoFactorService;

    public UserTwoFactorController(
        UserTwoFactorService userTwoFactorService) {
        this.userTwoFactorService = userTwoFactorService;
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<Map<String, String>> setupTwoFactorAuthentication(@RequestBody Map<String, String> request) {
        String accountIdString = request.get("accountId");
    
        if (accountIdString == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing accountId in the request body."));
        }
    
        try {
            UUID accountId = UUID.fromString(accountIdString);
            Map<String, String> response = userTwoFactorService.setupTwoFactorAuthentication(accountId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid accountId format."));
        }
    }
    
    @PostMapping("/2fa/verify")
    public ResponseEntity<Map<String, Object>> verifyTwoFactorAuthentication(@RequestBody Map<String, String> request) {
        String accountIdString = request.get("accountId");
        String otpString = request.get("otp");
    
        if (accountIdString == null || otpString == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing accountId or OTP in the request body."));
        }
    
        try {
            UUID accountId = UUID.fromString(accountIdString);
            int otp = Integer.parseInt(otpString);
            Map<String, Object> response = userTwoFactorService.verifyTwoFactorAuthentication(accountId, otp);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP format."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}