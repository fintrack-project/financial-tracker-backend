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
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender; // For sending emails

    @Value("${app.base-url}")
    private String baseUrl; // Base URL for email verification link

    public UserService(
        UserRepository userRepository, 
        JwtService jwtService,
        BCryptPasswordEncoder passwordEncoder,
        JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
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
        sendVerificationEmail(user.getEmail(), user);

        return "User registered successfully.";
    }

    private void setDefaultValues(User user) {
        user.setAccountTier("free");
        user.setSignupDate(LocalDateTime.now());
    }

    private void sendVerificationEmail(String email, User user) {
        // Generate a JWT for email verification
        String token = jwtService.generateVerificationToken(user.getUserId().toString());
        String verificationLink = baseUrl + "/verify-email?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Email Verification");
            helper.setText("<p>Please verify your email by clicking the link below:</p>" +
                    "<a href=\"" + verificationLink + "\">Verify Email</a>", true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> fetchUserDetails(UUID accountId) {
        return userRepository.findByAccountId(accountId);
    }

    @Transactional
    public String verifyEmail(String token) {
        try {
            // Parse the JWT
            String userId = jwtService.decodeToken(token);
    
            // Find the user by userId
            Optional<User> user = userRepository.findByUserId(userId);
            if (user.isEmpty()) {
                return "Invalid token.";
            }
    
            User existingUser = user.get();
            if (existingUser.getEmailVerified()) {
                return "Email already verified.";
            }
    
            // Activate the user's account
            existingUser.setEmailVerified(true);
            userRepository.save(existingUser);
    
            return "Email verified successfully.";
        } catch (Exception e) {
            return "Invalid or expired token.";
        }
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

    public void sendEmailVerification(UUID accountId, String email) {
        // Validate that the user exists
        Optional<User> userOptional = userRepository.findById(accountId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found with accountId: " + accountId);
        }

        // Send the verification email
        User user = userOptional.get();
        sendVerificationEmail(email, user);
    }
}