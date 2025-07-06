package com.fintrack.service.user;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;

import com.fintrack.model.user.User;
import com.fintrack.repository.user.UserRepository;
import com.fintrack.security.JwtService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

@Service
public class UserEmailService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender; // For sending emails

    @Value("${app.base-url}")
    private String baseUrl; // Base URL for email verification link

    @Value("${spring.mail.from}")
    private String fromEmail; // From email address

    public UserEmailService(
        UserRepository userRepository, 
        JwtService jwtService,
        BCryptPasswordEncoder passwordEncoder,
        JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String email, User user) {
        // Generate a JWT for email verification
        String token = jwtService.generateVerificationToken(user.getUserId().toString());
        String verificationLink = baseUrl + "/verify-email?token=" + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail); // Set explicit From address
            helper.setTo(email);
            helper.setSubject("Verify Your Email Address");
            
            // Load and process email template
            String htmlContent = loadEmailTemplate(user.getUserId(), verificationLink);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String loadEmailTemplate(String userName, String verificationLink) {
        try {
            // Load template from resources
            String template = new String(Files.readAllBytes(
                Paths.get(getClass().getClassLoader().getResource("templates/email-verification.html").toURI())
            ));
            
            // Replace placeholders
            return template
                .replace("{{userName}}", userName)
                .replace("{{verificationLink}}", verificationLink);
                
        } catch (Exception e) {
            logger.error("Failed to load email template", e);
            // Fallback to simple template if file loading fails
            return String.format(
                "<p>Hello %s,</p>" +
                "<p>Please verify your email by clicking the link below:</p>" +
                "<a href=\"%s\">Verify Email</a>",
                userName, verificationLink
            );
        }
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
            if (existingUser.isEmailVerified()) {
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
    public boolean isEmailVerified(UUID accountId) {
        // Find the user by accountId
        Optional<User> userOptional = userRepository.findByAccountId(accountId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found with accountId: " + accountId);
        }
    
        User user = userOptional.get();
        return user.isEmailVerified();
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