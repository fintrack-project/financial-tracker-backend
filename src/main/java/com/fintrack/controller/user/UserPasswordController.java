package com.fintrack.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fintrack.common.ApiResponse;
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
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.error("Missing accountId or password in the request body."));
            }
        
            UUID accountId = UUID.fromString(accountIdString);
            Map<String, Object> response = userPasswordService.verifyPasswordAuthentication(accountId, password);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error verifying password: ", e);
            return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ApiResponse.error("Failed to verify password"));
        }
    }
}