package com.fintrack.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fintrack.common.ApiResponse;
import com.fintrack.service.user.UserEmailService;

import java.util.*;

@RestController
@RequestMapping(value = "/api/user/email", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserEmailController {

    private static final Logger logger = LoggerFactory.getLogger(UserEmailController.class);
    private final UserEmailService userEmailService;

    public UserEmailController(UserEmailService userEmailService) {
        this.userEmailService = userEmailService;
    }

    @PostMapping(
        value = "/verify",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestBody Map<String, String> requestBody) {
        try {
            String token = requestBody.get("token");
            if (token == null) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Missing token in the request body."));
            }
        
            String result = userEmailService.verifyEmail(token);
            if (result.equals("Email verified successfully.")) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.success(result));
            } else {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error(result));
            }
        } catch (Exception e) {
            logger.error("Error verifying email: ", e);
            return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Failed to verify email"));
        }
    }

    @PostMapping(
        value = "/send-verification",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Void>> sendEmailVerification(@RequestBody Map<String, Object> request) {
        try {
            // Extract accountId and email from the request
            String accountIdString = (String) request.get("accountId");
            String email = (String) request.get("email");

            if (accountIdString == null || email == null) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Missing accountId or email"));
            }

            // Convert accountId to UUID
            UUID accountId = UUID.fromString(accountIdString);

            // Call the service to send the email verification
            userEmailService.sendEmailVerification(accountId, email);

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Verification email sent successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Invalid accountId format"));
        } catch (Exception e) {
            logger.error("Error sending verification email: ", e);
            return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Failed to send verification email"));
        }
    }

    @PostMapping(
        value = "/verified",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Boolean>> checkEmailVerified(@RequestBody Map<String, String> requestBody) {
        try {
            String accountIdStr = requestBody.get("accountId");
            if (accountIdStr == null) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Missing accountId in the request body."));
            }
        
            UUID accountId = UUID.fromString(accountIdStr);
            boolean isVerified = userEmailService.isEmailVerified(accountId);
        
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(isVerified));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Invalid accountId format."));
        } catch (Exception e) {
            logger.error("Error checking email verification: ", e);
            return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Failed to check email verification status"));
        }
    }
}