package com.fintrack.controller.user;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.model.user.User;
import com.fintrack.service.user.UserService;
import com.fintrack.security.JwtService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@RestController
@RequestMapping(value = "/api/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
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
            return ResponseWrapper.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Login error: ", e);
            return ResponseWrapper.internalServerError("An unexpected error occurred");
        }
    }

    @PostMapping(
        value = "/register",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<String>> registerUser(@RequestBody User user) {
        try {
            String result = userService.registerUser(user);
            return ResponseWrapper.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Registration error: ", e);
            return ResponseWrapper.internalServerError("An unexpected error occurred during registration");
        }
    }

    @PostMapping(
        value = "/refresh",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(@RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null) {
                return ResponseWrapper.badRequest("Refresh token is required");
            }

            String newAccessToken = jwtService.refreshAccessToken(refreshToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", newAccessToken);
            response.put("refreshToken", refreshToken); // Return the same refresh token
            
            return ResponseWrapper.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
        } catch (Exception e) {
            logger.error("Token refresh error: ", e);
            return ResponseWrapper.internalServerError("Failed to refresh token");
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
            return ResponseWrapper.badRequest("Missing accountId in the request body.");
        }
    
        try {
            UUID accountId = UUID.fromString(accountIdStr);
            Optional<User> user = userService.fetchUserDetails(accountId);
    
            if (user.isPresent()) {
                return ResponseWrapper.ok(user.get());
            } else {
                return ResponseWrapper.notFound("User not found");
            }
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest("Invalid accountId format");
        } catch (Exception e) {
            logger.error("Error fetching user details: ", e);
            return ResponseWrapper.internalServerError("Failed to fetch user details");
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

            return ResponseWrapper.ok(null, "Phone number updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
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

            return ResponseWrapper.ok(null, "Address updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
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

            return ResponseWrapper.ok(null, "Email updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
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
            return ResponseWrapper.badRequest("Missing accountId or newPassword.");
        }

        try {
            userService.updateUserPassword(UUID.fromString(accountId), newPassword);
            return ResponseWrapper.ok("Password updated successfully.");
        } catch (Exception e) {
            return ResponseWrapper.internalServerError("Failed to update password.");
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
            return ResponseWrapper.badRequest("Missing accountId or enabled status.");
        }
    
        try {
            userService.updateUserTwoFactorStatus(UUID.fromString(accountId), enabled);
            return ResponseWrapper.ok(null, "2FA status updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest(e.getMessage());
        }
    }
}