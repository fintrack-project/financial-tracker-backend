package com.fintrack.service;

import com.fintrack.model.User;
import com.fintrack.repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String registerUser(User user) {
        // Check if user ID already exists
        if (userRepository.findByUserId(user.getUserId()).isPresent()) {
            return "User ID already exists.";
        }

        // Check if user email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return "Email already exists.";
        }

        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Save the user
        userRepository.save(user);

        return "User registered successfully.";
    }

    @Transactional(readOnly = true)
    public Optional<User> fetchUserDetails(UUID accountId) {
        return userRepository.findByAccountId(accountId);
    }
}