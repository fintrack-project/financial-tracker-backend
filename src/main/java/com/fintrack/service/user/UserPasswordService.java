package com.fintrack.service.user;

import com.fintrack.model.user.User;
import com.fintrack.repository.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserPasswordService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> verifyPasswordAuthentication(UUID accountId, String password) {
        Optional<User> userOptional = userRepository.findById(accountId);

        if (userOptional.isEmpty()) {
            return Map.of("success", false, "message", "User not found.");
        }

        User user = userOptional.get();

        // Verify the password
        if (passwordEncoder.matches(password, user.getPassword())) {
            return Map.of("success", true, "message", "Password verified successfully.");
        } else {
            return Map.of("success", false, "message", "Invalid password.");
        }
    }
}