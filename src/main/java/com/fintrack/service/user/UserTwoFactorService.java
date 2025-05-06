package com.fintrack.service.user;

import com.fintrack.model.user.User;
import com.fintrack.repository.user.UserRepository;
import com.fintrack.security.TwoFactorService;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public class UserTwoFactorService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final TwoFactorService twoFactorService;

    public UserTwoFactorService(
        UserRepository userRepository,
        TwoFactorService twoFactorService) {
        this.userRepository = userRepository;
        this.twoFactorService = twoFactorService;
    }

    public Map<String, String> setupTwoFactorAuthentication(UUID accountId) {
        Optional<User> userOptional = userRepository.findById(accountId);
    
        if (userOptional.isPresent()) {
            User user = userOptional.get();
    
            // Generate 2FA secret
            GoogleAuthenticatorKey key = twoFactorService.generateSecret();
            String secret = key.getKey();
    
            // Save the secret to the user
            user.setTwoFactorSecret(secret);
            userRepository.save(user);
    
            // Generate QR code
            String qrCode = twoFactorService.generateQRCode(user.getEmail(), key);
    
            return Map.of("secret", secret, "qrCode", qrCode);
        } else {
            throw new IllegalArgumentException("User not found.");
        }
    }
    
    public Map<String, Object> verifyTwoFactorAuthentication(UUID accountId, int otp) {
        Optional<User> userOptional = userRepository.findById(accountId);
    
        if (userOptional.isPresent()) {
            User user = userOptional.get();
    
            // Verify OTP
            boolean isValid = twoFactorService.verifyOTP(user.getTwoFactorSecret(), otp);
    
            if (isValid) {
                user.setTwoFactorEnabled(true);
                userRepository.save(user);
                return Map.of("success", true, "message", "OTP verified successfully.");
            } else {
                return Map.of("success", false, "message", "Invalid OTP.");
            }
        } else {
            throw new IllegalArgumentException("User not found.");
        }
    }
}