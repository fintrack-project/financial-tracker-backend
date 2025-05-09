package com.fintrack.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.service.user.UserPasswordService;

import java.util.*;

@RestController
@RequestMapping(value = "/api/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(UserPasswordController.class);
    private final UserPasswordService userPasswordService;

    public UserPasswordController(UserPasswordService userPasswordService) {
        this.userPasswordService = userPasswordService;
    }
    
    @PostMapping(
        value = "/password/verify",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPasswordAuthentication(@RequestBody Map<String, String> request) {
        try {
            String accountIdString = request.get("accountId");
            String password = request.get("password");
        
            if (accountIdString == null || password == null) {
                return ResponseWrapper.badRequest("Missing accountId or password in the request body.");
            }
        
            UUID accountId = UUID.fromString(accountIdString);
            Map<String, Object> response = userPasswordService.verifyPasswordAuthentication(accountId, password);
            return ResponseWrapper.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Error verifying password: ", e);
            return ResponseWrapper.internalServerError("Failed to verify password");
        }
    }
    
    @PostMapping(
        value = "/password-reset-request",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestPasswordReset(@RequestBody Map<String, String> requestBody) {
        try {
            String identifier = requestBody.get("identifier");
            
            if (identifier == null || identifier.trim().isEmpty()) {
                return ResponseWrapper.badRequest("Missing identifier (userId or email) in the request.");
            }
            
            Map<String, Object> response = userPasswordService.requestPasswordReset(identifier);
            return ResponseWrapper.ok(response);
        } catch (Exception e) {
            logger.error("Error requesting password reset: ", e);
            return ResponseWrapper.internalServerError("Failed to process password reset request");
        }
    }
    
    @GetMapping(
        value = "/validate-reset-token/{token}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateResetToken(@PathVariable String token) {
        try {
            boolean isValid = userPasswordService.validateResetToken(token);
            
            if (isValid) {
                return ResponseWrapper.ok(Map.of("valid", true));
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "Invalid or expired token");
                return ResponseWrapper.badRequest("Invalid or expired token");
            }
        } catch (Exception e) {
            logger.error("Error validating reset token: ", e);
            return ResponseWrapper.internalServerError("Failed to validate token");
        }
    }
    
    @PostMapping(
        value = "/reset-password",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(@RequestBody Map<String, String> requestBody) {
        try {
            String token = requestBody.get("token");
            String newPassword = requestBody.get("newPassword");
            
            if (token == null || newPassword == null) {
                return ResponseWrapper.badRequest("Missing token or newPassword in the request body.");
            }
            
            Map<String, Object> response = userPasswordService.resetPassword(token, newPassword);
            
            if ((boolean) response.get("success")) {
                return ResponseWrapper.ok(response);
            } else {
                return ResponseWrapper.badRequest((String) response.get("message"));
            }
        } catch (Exception e) {
            logger.error("Error resetting password: ", e);
            return ResponseWrapper.internalServerError("Failed to reset password");
        }
    }
}