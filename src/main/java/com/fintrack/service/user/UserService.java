package com.fintrack.service.user;

import com.fintrack.model.user.User;
import com.fintrack.repository.user.UserRepository;
import com.fintrack.security.JwtService;
import com.fintrack.service.subscription.UserSubscriptionService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserEmailService userEmailService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserSubscriptionService userSubscriptionService;

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_TIME_DURATION = 1; // in minutes

    public UserService(
        UserRepository userRepository,
        UserEmailService userEmailService,
        BCryptPasswordEncoder passwordEncoder,
        JwtService jwtService,
        UserSubscriptionService userSubscriptionService) {
        this.userRepository = userRepository;
        this.userEmailService = userEmailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userSubscriptionService = userSubscriptionService;
    }

    public Map<String, Object> authenticateAndGenerateToken(String userId, String password) {
        logger.trace("Starting authentication for userId: {}", userId);
        // Find the user by email
        Optional<User> userOptional = userRepository.findByUserId(userId);;
        if (userOptional.isEmpty()) {
            logger.warn("User not found for userId: {}", userId);
            throw new IllegalArgumentException("Invalid userId or password.");
        }

        User user = userOptional.get();
        logger.trace("User found for userId: {}", userId);

        // Check if the account is locked
        if (user.getAccountLocked()) {
            logger.warn("Account is locked for userId: {}", userId);
            // Check if the blocking time has passed
            if (isBlockingTimeOver(user)) {
                // Unlock the account if blocking time is over
                logger.trace("Blocking time is over. Unlocking account for userId: {}", userId);
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
            } else {
                // Account is still locked
                logger.warn("Account is still locked for userId: {}", userId);
                userRepository.save(user);
                throw new IllegalArgumentException(String.format("Account is locked. Please try again %s minutes later.", LOCK_TIME_DURATION));
            }
        }

        // Record last activity date
        user.setLastActivityDate(LocalDateTime.now());
        logger.trace("Last activity date updated for userId: {}", userId);

        // Authenticate the user
        if (passwordEncoder.matches(password, user.getPassword())) {
            logger.trace("Password matched for userId: {}", userId);

            // Generate JWT token
            String token = jwtService.generateVerificationToken(user.getUserId().toString());
            logger.trace("JWT token generated for userId: {}", userId);

            // Successful login
            user.setFailedLoginAttempts(0); // Reset failed login attempts
            user.setLastLogin(LocalDateTime.now()); // Record last login
            user.setAccountLocked(false); // Unlock account if it was locked
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", user.getUserId());
            response.put("accountId", user.getAccountId());
            return response;
        } else {
            // Failed login
            logger.warn("Invalid password for userId: {}", userId);
            incrementFailedLoginAttempts(user);
            userRepository.save(user);

            // Validate the password
            throw new IllegalArgumentException("Invalid userId or password.");
        }
    }

    private void incrementFailedLoginAttempts(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            user.setAccountLocked(true); // Lock the account
        }
    }

    private boolean isBlockingTimeOver(User user) {
        LocalDateTime lastActivity = user.getLastActivityDate();
        if (lastActivity == null) {
            return true; // No activity recorded, allow login
        }

        // Check if the blocking time has passed
        return ChronoUnit.MINUTES.between(lastActivity, LocalDateTime.now()) >= LOCK_TIME_DURATION;
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
        
        // Create a free subscription for the new user
        try {
            userSubscriptionService.createFreeSubscription(user.getAccountId(), "Free");
            logger.info("Created free subscription for new user with accountId: {}", user.getAccountId());
        } catch (Exception e) {
            logger.error("Failed to create free subscription for user: {}", e.getMessage(), e);
            // Continue registration process even if subscription creation fails
        }

        // Send the verification email
        userEmailService.sendVerificationEmail(user.getEmail(), user);

        return "User registered successfully.";
    }

    private void setDefaultValues(User user) {
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
            user.setPhoneVerified(false); // Reset phone verification status
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("User not found with accountId: " + accountId);
        }
    }

    public boolean setPhoneVerified(UUID accountId) {
        Optional<User> userOptional = userRepository.findById(accountId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPhoneVerified(true);
            userRepository.save(user);
            return true;
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

    public void updateUserPassword(UUID accountId, String newPassword) {
        Optional<User> userOptional = userRepository.findById(accountId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("User not found with accountId: " + accountId);
        }
    }

    public void updateUserTwoFactorStatus(UUID accountId, boolean enabled) {
        Optional<User> userOptional = userRepository.findById(accountId);
    
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setTwoFactorEnabled(enabled);
            userRepository.save(user);
            logger.info("Updated 2FA status for accountId: {} to {}", accountId, enabled);
        } else {
            logger.warn("User not found for accountId: {}", accountId);
            throw new IllegalArgumentException("User not found.");
        }
    }
}