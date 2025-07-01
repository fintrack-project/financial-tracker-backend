package com.fintrack.service.user;

import com.fintrack.model.user.PasswordResetToken;
import com.fintrack.model.user.User;
import com.fintrack.repository.user.PasswordResetTokenRepository;
import com.fintrack.repository.user.UserRepository;
import com.fintrack.security.JwtService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserPasswordService {

    private static final Logger logger = LoggerFactory.getLogger(UserPasswordService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender mailSender;
    private final JwtService jwtService;

    @Value("${app.base-url}")
    private String baseUrl; // Base URL for reset link

    @Value("${app.reset-token-expiry}")
    private int resetTokenExpiryMinutes; // Expiry duration in minutes

    @Value("${spring.mail.from}")
    private String fromEmail; // From email address

    @Autowired
    public UserPasswordService(
            UserRepository userRepository, 
            PasswordEncoder passwordEncoder,
            PasswordResetTokenRepository passwordResetTokenRepository,
            JavaMailSender mailSender,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailSender = mailSender;
        this.jwtService = jwtService;
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
    
    @Transactional
    public Map<String, Object> requestPasswordReset(String identifier) {
        logger.info("Password reset requested for identifier: {}", identifier);
        
        // Find user by either userId or email
        Optional<User> userOptional = userRepository.findByUserId(identifier);
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByEmail(identifier);
        }
        
        if (userOptional.isEmpty()) {
            logger.warn("No user found with identifier: {}", identifier);
            // For security reasons, don't reveal if the user exists or not
            return Map.of("success", true, "message", "If the account exists, a password reset link will be sent.");
        }
        
        User user = userOptional.get();
        
        // Check if user already has an active reset token
        passwordResetTokenRepository.findByUserIdAndUsedFalse(user.getUserId())
            .ifPresent(token -> {
                token.setUsed(true); // Invalidate previous tokens
                passwordResetTokenRepository.save(token);
            });
        
        // Generate a JWT token
        String jwtToken = jwtService.generateVerificationToken(user.getUserId());
        
        // Set expiry time for the password reset token
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(resetTokenExpiryMinutes);
        
        // Create and save new password reset token
        PasswordResetToken resetToken = new PasswordResetToken(jwtToken, user.getUserId(), expiryTime);
        passwordResetTokenRepository.save(resetToken);
        
        // Send email with reset link
        sendPasswordResetEmail(user.getEmail(), jwtToken);
        
        logger.info("Password reset token generated for user: {}", user.getUserId());
        return Map.of("success", true, "message", "Password reset link has been sent to your email.");
    }
    
    @Transactional
    public boolean validateResetToken(String token) {
        logger.info("Validating reset token: {}", token);
        
        Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository.findByToken(token);
        
        if (tokenOptional.isEmpty()) {
            logger.warn("Token not found: {}", token);
            return false;
        }
        
        PasswordResetToken resetToken = tokenOptional.get();
        
        // Check if token is expired or already used
        if (resetToken.isExpired() || resetToken.isUsed()) {
            logger.warn("Token is expired or used: {}", token);
            return false;
        }
        
        return true;
    }
    
    @Transactional
    public Map<String, Object> resetPassword(String token, String newPassword) {
        logger.info("Password reset attempted with token: {}", token);
        
        // Find the reset token
        Optional<PasswordResetToken> resetTokenOptional = passwordResetTokenRepository.findByToken(token);
        
        if (resetTokenOptional.isEmpty()) {
            logger.warn("Invalid reset token: {}", token);
            return Map.of("success", false, "message", "Invalid or expired reset token.");
        }
        
        PasswordResetToken resetToken = resetTokenOptional.get();
        
        // Check if token has expired or is already used
        if (resetToken.isExpired() || resetToken.isUsed()) {
            logger.warn("Expired or used reset token: {}", token);
            if (resetToken.isExpired()) {
                resetToken.setUsed(true);
                passwordResetTokenRepository.save(resetToken);
            }
            return Map.of("success", false, "message", "Reset token has expired or already been used.");
        }
        
        // Find the user
        Optional<User> userOptional = userRepository.findByUserId(resetToken.getUserId());
        if (userOptional.isEmpty()) {
            logger.warn("User not found for reset token: {}", token);
            return Map.of("success", false, "message", "User not found.");
        }
        
        User user = userOptional.get();
        
        // Update the password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Mark the token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        
        logger.info("Password reset successful for user: {}", user.getUserId());
        return Map.of("success", true, "message", "Password reset successfully.");
    }
    
    private void sendPasswordResetEmail(String email, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail); // Set explicit From address
            helper.setTo(email);
            helper.setSubject("Password Reset Request");
            helper.setText(
                "<p>You have requested to reset your password. Please click the link below to reset your password:</p>" +
                "<a href=\"" + resetLink + "\">Reset Password</a>" +
                "<p>This link will expire in " + resetTokenExpiryMinutes + " minutes.</p>" +
                "<p>If you did not request a password reset, please ignore this email or contact support if you have concerns.</p>", 
                true
            );
            
            mailSender.send(message);
            logger.info("Password reset email sent to: {}", email);
        } catch (MessagingException e) {
            logger.error("Failed to send password reset email: {}", e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}