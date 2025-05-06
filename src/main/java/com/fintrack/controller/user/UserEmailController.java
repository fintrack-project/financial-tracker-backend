package com.fintrack.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fintrack.service.user.UserEmailService;

import java.util.*;

@RestController
@RequestMapping("/api/user/email")
public class UserEmailController {

    private final UserEmailService userEmailService;

    public UserEmailController(
        UserEmailService userEmailService) {
        this.userEmailService = userEmailService;
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> requestBody) {
        String token = requestBody.get("token");
        if (token == null) {
            return ResponseEntity.badRequest().body("Missing token in the request body.");
        }
    
        String result = userEmailService.verifyEmail(token);
        if (result.equals("Email verified successfully.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/send-verification")
    public ResponseEntity<Void> sendEmailVerification(@RequestBody Map<String, Object> request) {
        // Extract accountId and email from the request
        String accountIdString = (String) request.get("accountId");
        String email = (String) request.get("email");

        // Convert accountId to UUID
        UUID accountId = UUID.fromString(accountIdString);

        // Call the service to send the email verification
        userEmailService.sendEmailVerification(accountId, email);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/verified")
    public ResponseEntity<?> checkEmailVerified(@RequestBody Map<String, String> requestBody) {
        String accountIdStr = requestBody.get("accountId");
        if (accountIdStr == null) {
            return ResponseEntity.badRequest().body("Missing accountId in the request body.");
        }
    
        try {
            UUID accountId = UUID.fromString(accountIdStr);
            boolean isVerified = userEmailService.isEmailVerified(accountId);
    
            return ResponseEntity.ok(isVerified);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid accountId format.");
        }
    }
}