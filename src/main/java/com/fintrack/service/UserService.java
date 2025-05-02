package com.fintrack.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;

import javax.crypto.SecretKey;

import com.fintrack.model.User;
import com.fintrack.repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender; // For sending emails

    private final SecretKey signingKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration; // JWT expiration time
    @Value("${app.base-url}")
    private String baseUrl; // Base URL for email verification link

    public UserService(
        UserRepository userRepository, 
        BCryptPasswordEncoder passwordEncoder,
        JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
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

        // Set emailVerified to false
        user.setEmailVerified(false);

        // Save the user
        userRepository.save(user);

        // Generate a JWT for email verification
        String token = generateVerificationToken(user);

        // Send the verification email
        sendVerificationEmail(user.getEmail(), token);

        return "User registered successfully.";
    }

    private String generateVerificationToken(User user) {

        return Jwts.builder()
                .setSubject(user.getAccountId().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                // .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .signWith(signingKey)
                .compact();
    }

    private void sendVerificationEmail(String email, String token) {
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

    @Transactional
    public String verifyEmail(String token) {
        try {
            // Parse the JWT
            String accountId = Jwts.parser()
                    .setSigningKey(signingKey)
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
    
            // Find the user by accountId
            Optional<User> user = userRepository.findByAccountId(UUID.fromString(accountId));
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

    @Transactional(readOnly = true)
    public Optional<User> fetchUserDetails(UUID accountId) {
        return userRepository.findByAccountId(accountId);
    }
}