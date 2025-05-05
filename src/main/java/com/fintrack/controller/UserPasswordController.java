package com.fintrack.controller;

import com.fintrack.service.UserPasswordService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserPasswordController {

    private final UserPasswordService userPasswordService;

    public UserPasswordController(
        UserPasswordService userPasswordService) {
        this.userPasswordService = userPasswordService;
    }
    
    @PostMapping("/password/verify")
    public ResponseEntity<Map<String, Object>> verifyPasswordAuthentication(@RequestBody Map<String, String> request) {
        String accountIdString = request.get("accountId");
        String password = request.get("password");
    
        if (accountIdString == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing accountId or password in the request body."));
        }
    
        try {
            UUID accountId = UUID.fromString(accountIdString);
            Map<String, Object> response = userPasswordService.verifyPasswordAuthentication(accountId, password);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}