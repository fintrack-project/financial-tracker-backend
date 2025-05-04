package com.fintrack.controller;

import com.fintrack.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/user/phone")
public class UserPhoneController {    

    private final UserService userService;
    public UserPhoneController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/verified")
    public ResponseEntity<Boolean> sendPhoneVerified(@RequestBody Map<String, String> request) {
        String accountId = request.get("accountId");

        // Call the service to update the phoneVerified field
        boolean isUpdated = userService.setPhoneVerified(UUID.fromString(accountId));

        return ResponseEntity.ok(isUpdated);
    }
}