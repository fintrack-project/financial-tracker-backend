package com.fintrack.service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;

import com.fintrack.model.User;
import com.fintrack.repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserEmailService userEmailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(
        UserRepository userRepository,
        UserEmailService userEmailService,
        BCryptPasswordEncoder passwordEncoder,
        JwtService jwtService) {
        this.userRepository = userRepository;
        this.userEmailService = userEmailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String authenticateAndGenerateToken(String userId, String password) {
        // Find the user by email
        Optional<User> userOptional = userRepository.findByUserId(userId);;
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid userId or password.");
        }

        User user = userOptional.get();

        // Validate the password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid userId or password.");
        }

        return jwtService.generateVerificationToken(user.getUserId().toString());
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

        setDefaultValues(user);

        // Set emailVerified to false
        user.setEmailVerified(false);

        // Save the user
        userRepository.save(user);

        // Send the verification email
        userEmailService.sendVerificationEmail(user.getEmail(), user);

        return "User registered successfully.";
    }

    private void setDefaultValues(User user) {
        user.setAccountTier("free");
        user.setSignupDate(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public Optional<User> fetchUserDetails(UUID accountId) {
        return userRepository.findByAccountId(accountId);
    }

    public void updateUserPhone(UUID accountId, String phone, String countryCode) {
        Optional<User> userOptional = userRepository.findById(accountId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPhone(phone);
            user.setCountryCode(countryCode);
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("User not found with accountId: " + accountId);
        }
    }

    public void updateUserAddress(UUID accountId, String address) {
        Optional<User> userOptional = userRepository.findById(accountId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setAddress(address);
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("User not found with accountId: " + accountId);
        }
    }

    public void updateUserEmail(UUID accountId, String email) {
        Optional<User> userOptional = userRepository.findById(accountId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setEmail(email);
            user.setEmailVerified(false); // Reset email verification status
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("User not found with accountId: " + accountId);
        }
    }
}