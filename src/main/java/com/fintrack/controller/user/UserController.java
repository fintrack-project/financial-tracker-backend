package com.fintrack.controller.user;

import com.fintrack.common.ApiResponse;
import com.fintrack.model.user.User;
import com.fintrack.service.user.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping(value = "/api/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(
        value = "/login",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginUser(@RequestBody Map<String, String> loginRequest) {
        try {
            String userId = loginRequest.get("userId");
            String password = loginRequest.get("password");
            Map<String, Object> response = userService.authenticateAndGenerateToken(userId, password);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Login error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("An unexpected error occurred"));
        }
    }

    @PostMapping(
        value = "/register",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<String>> registerUser(@RequestBody User user) {
        String result = userService.registerUser(user);
        if (result.equals("User registered successfully.")) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(result));
        } else {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(result));
        }
    }

    @PostMapping(
        value = "/fetch",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<User>> fetchUserDetails(@RequestBody Map<String, String> requestBody) {
        String accountIdStr = requestBody.get("accountId");
        if (accountIdStr == null) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Missing accountId in the request body."));
        }
    
        try {
            UUID accountId = UUID.fromString(accountIdStr);
            Optional<User> user = userService.fetchUserDetails(accountId);
    
            if (user.isPresent()) {
                return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.success(user.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("User not found"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Invalid accountId format"));
        } catch (Exception e) {
            logger.error("Error fetching user details: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Failed to fetch user details"));
        }
    }

    @PostMapping(
        value = "/update-phone",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Void>> updateUserPhone(@RequestBody Map<String, Object> request) {
        try {
            String accountIdString = (String) request.get("accountId");
            String phone = (String) request.get("phone");
            String countryCode = (String) request.get("countryCode");

            UUID accountId = UUID.fromString(accountIdString);
            userService.updateUserPhone(accountId, phone, countryCode);

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Phone number updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping(
        value = "/update-address",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Void>> updateUserAddress(@RequestBody Map<String, Object> request) {
        try {
            String accountIdString = (String) request.get("accountId");
            String address = (String) request.get("address");

            UUID accountId = UUID.fromString(accountIdString);
            userService.updateUserAddress(accountId, address);

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Address updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping(
        value = "/update-email",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Void>> updateUserEmail(@RequestBody Map<String, Object> request) {
        try {
            String accountId = (String) request.get("accountId");
            String email = (String) request.get("email");

            userService.updateUserEmail(UUID.fromString(accountId), email);

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "Email updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping(
        value = "/update-password",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<String>> updateUserPassword(@RequestBody Map<String, String> requestBody) {
        String accountId = requestBody.get("accountId");
        String newPassword = requestBody.get("newPassword");

        if (accountId == null || newPassword == null) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Missing accountId or newPassword."));
        }

        try {
            userService.updateUserPassword(UUID.fromString(accountId), newPassword);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success("Password updated successfully."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Failed to update password."));
        }
    }

    @PostMapping(
        value = "/update-2fa",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Void>> updateUserTwoFactorStatus(@RequestBody Map<String, Object> request) {
        String accountId = (String) request.get("accountId");
        Boolean enabled = (Boolean) request.get("enabled");
    
        if (accountId == null || enabled == null) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Missing accountId or enabled status."));
        }
    
        try {
            userService.updateUserTwoFactorStatus(UUID.fromString(accountId), enabled);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(null, "2FA status updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}