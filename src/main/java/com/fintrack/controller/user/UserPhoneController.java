package com.fintrack.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fintrack.common.ApiResponse;
import com.fintrack.common.ResponseWrapper;
import com.fintrack.service.user.UserService;

import java.util.*;

@RestController
@RequestMapping(value = "/api/user/phone", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserPhoneController {    

    private static final Logger logger = LoggerFactory.getLogger(UserPhoneController.class);
    private final UserService userService;

    public UserPhoneController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(
        value = "/verified",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<Boolean>> sendPhoneVerified(@RequestBody Map<String, String> request) {
        try {
            String accountId = request.get("accountId");

            if (accountId == null) {
                return ResponseWrapper.badRequest("Missing accountId in the request body.");
            }

            // Call the service to update the phoneVerified field
            boolean isUpdated = userService.setPhoneVerified(UUID.fromString(accountId));

            return ResponseWrapper.ok(isUpdated);
        } catch (IllegalArgumentException e) {
            return ResponseWrapper.badRequest("Invalid accountId format");
        } catch (Exception e) {
            logger.error("Error verifying phone: ", e);
            return ResponseWrapper.internalServerError("Failed to verify phone");
        }
    }
}