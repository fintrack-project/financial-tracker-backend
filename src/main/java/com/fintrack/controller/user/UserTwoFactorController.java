package com.fintrack.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fintrack.common.ApiResponse;
import com.fintrack.service.user.UserTwoFactorService;

import java.util.*;

@RestController
@RequestMapping(value = "/api/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserTwoFactorController {

    private static final Logger logger = LoggerFactory.getLogger(UserTwoFactorController.class);
    private final UserTwoFactorService userTwoFactorService;

    public UserTwoFactorController(UserTwoFactorService userTwoFactorService) {
        this.userTwoFactorService = userTwoFactorService;
    }

    @PostMapping(
        value = "/2fa/setup",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> setupTwoFactorAuthentication(@RequestBody Map<String, String> request) {
        try {
            String accountIdString = request.get("accountId");
        
            if (accountIdString == null) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Missing accountId in the request body."));
            }
        
            UUID accountId = UUID.fromString(accountIdString);
            Map<String, String> response = userTwoFactorService.setupTwoFactorAuthentication(accountId);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Invalid accountId format."));
        } catch (Exception e) {
            logger.error("Error setting up 2FA: ", e);
            return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Failed to set up 2FA"));
        }
    }
    
    @PostMapping(
        value = "/2fa/verify",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyTwoFactorAuthentication(@RequestBody Map<String, String> request) {
        try {
            String accountIdString = request.get("accountId");
            String otpString = request.get("otp");
        
            if (accountIdString == null || otpString == null) {
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Missing accountId or OTP in the request body."));
            }
        
            UUID accountId = UUID.fromString(accountIdString);
            int otp = Integer.parseInt(otpString);
            Map<String, Object> response = userTwoFactorService.verifyTwoFactorAuthentication(accountId, otp);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(response));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Invalid OTP format."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error verifying 2FA: ", e);
            return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Failed to verify 2FA"));
        }
    }
}