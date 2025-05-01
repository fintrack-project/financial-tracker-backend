package com.fintrack.controller;

import com.fintrack.model.User;
import com.fintrack.service.UserService;
import com.fintrack.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private UserRepository userRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private final UserService userService;

    public UserController(
        UserRepository userRepository, 
        BCryptPasswordEncoder passwordEncoder, 
        UserService userService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody User loginRequest, HttpServletRequest request) {
        Optional<User> user = userRepository.findByUserId(loginRequest.getUserId());

        if (user.isPresent() && passwordEncoder.matches(loginRequest.getPassword(), user.get().getPassword())) {
            // Store userId in the session
            request.getSession().setAttribute("userId", loginRequest.getUserId());
            return ResponseEntity.ok("Login successful!");
        } else {
            return ResponseEntity.status(401).body("Invalid user ID or password.");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        String result = userService.registerUser(user);

        if (result.equals("User registered successfully.")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/fetch")
    public ResponseEntity<?> fetchUserDetails(@RequestParam UUID accountId) {
        Optional<User> user = userService.fetchUserDetails(accountId);

        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.status(404).body("User not found.");
        }
    }
}