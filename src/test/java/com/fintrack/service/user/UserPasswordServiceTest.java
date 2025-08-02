package com.fintrack.service.user;

import com.fintrack.model.user.PasswordResetToken;
import com.fintrack.model.user.User;
import com.fintrack.repository.user.PasswordResetTokenRepository;
import com.fintrack.repository.user.UserRepository;
import com.fintrack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPasswordService Tests")
class UserPasswordServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private JwtService jwtService;

    private UserPasswordService userPasswordService;

    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
    private static final String TEST_TOKEN = "test-reset-token";

    @BeforeEach
    void setUp() {
        userPasswordService = new UserPasswordService(
            userRepository,
            passwordEncoder,
            passwordResetTokenRepository,
            mailSender,
            jwtService
        );
    }

    @Test
    @DisplayName("Should verify password authentication successfully")
    void shouldVerifyPasswordAuthenticationSuccessfully() {
        // Given: Valid user and password
        User user = createTestUser();
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(true);

        // When: Verifying password authentication
        Map<String, Object> result = userPasswordService.verifyPasswordAuthentication(TEST_ACCOUNT_ID, TEST_PASSWORD);

        // Then: Should return success
        assertTrue((Boolean) result.get("success"));
        assertEquals("Password verified successfully.", result.get("message"));
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verify(passwordEncoder).matches(TEST_PASSWORD, user.getPassword());
    }

    @Test
    @DisplayName("Should fail password verification when user not found")
    void shouldFailPasswordVerificationWhenUserNotFound() {
        // Given: User not found
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // When: Verifying password authentication
        Map<String, Object> result = userPasswordService.verifyPasswordAuthentication(TEST_ACCOUNT_ID, TEST_PASSWORD);

        // Then: Should return failure
        assertFalse((Boolean) result.get("success"));
        assertEquals("User not found.", result.get("message"));
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Should fail password verification when password is invalid")
    void shouldFailPasswordVerificationWhenPasswordInvalid() {
        // Given: Valid user but invalid password
        User user = createTestUser();
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(false);

        // When: Verifying password authentication
        Map<String, Object> result = userPasswordService.verifyPasswordAuthentication(TEST_ACCOUNT_ID, TEST_PASSWORD);

        // Then: Should return failure
        assertFalse((Boolean) result.get("success"));
        assertEquals("Invalid password.", result.get("message"));
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verify(passwordEncoder).matches(TEST_PASSWORD, user.getPassword());
    }

    @Test
    @DisplayName("Should validate reset token successfully")
    void shouldValidateResetTokenSuccessfully() {
        // Given: Valid reset token
        PasswordResetToken resetToken = createTestResetToken();
        resetToken.setUsed(false);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        
        when(passwordResetTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(resetToken));

        // When: Validating reset token
        boolean result = userPasswordService.validateResetToken(TEST_TOKEN);

        // Then: Should return true
        assertTrue(result);
        verify(passwordResetTokenRepository).findByToken(TEST_TOKEN);
    }

    @Test
    @DisplayName("Should fail token validation when token not found")
    void shouldFailTokenValidationWhenTokenNotFound() {
        // Given: Token not found
        when(passwordResetTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.empty());

        // When: Validating reset token
        boolean result = userPasswordService.validateResetToken(TEST_TOKEN);

        // Then: Should return false
        assertFalse(result);
        verify(passwordResetTokenRepository).findByToken(TEST_TOKEN);
    }

    @Test
    @DisplayName("Should fail token validation when token is expired")
    void shouldFailTokenValidationWhenTokenExpired() {
        // Given: Expired reset token
        PasswordResetToken resetToken = createTestResetToken();
        resetToken.setUsed(false);
        resetToken.setExpiryDate(LocalDateTime.now().minusMinutes(30));
        
        when(passwordResetTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(resetToken));

        // When: Validating reset token
        boolean result = userPasswordService.validateResetToken(TEST_TOKEN);

        // Then: Should return false
        assertFalse(result);
        verify(passwordResetTokenRepository).findByToken(TEST_TOKEN);
    }

    @Test
    @DisplayName("Should fail token validation when token is used")
    void shouldFailTokenValidationWhenTokenUsed() {
        // Given: Used reset token
        PasswordResetToken resetToken = createTestResetToken();
        resetToken.setUsed(true);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        
        when(passwordResetTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(resetToken));

        // When: Validating reset token
        boolean result = userPasswordService.validateResetToken(TEST_TOKEN);

        // Then: Should return false
        assertFalse(result);
        verify(passwordResetTokenRepository).findByToken(TEST_TOKEN);
    }

    @Test
    @DisplayName("Should reset password successfully")
    void shouldResetPasswordSuccessfully() {
        // Given: Valid reset token and user
        PasswordResetToken resetToken = createTestResetToken();
        resetToken.setUsed(false);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        
        User user = createTestUser();
        
        when(passwordResetTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(resetToken));
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("new-encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(resetToken);

        // When: Resetting password
        Map<String, Object> result = userPasswordService.resetPassword(TEST_TOKEN, "newpassword");

        // Then: Should return success
        assertTrue((Boolean) result.get("success"));
        assertEquals("Password reset successfully.", result.get("message"));
        verify(passwordResetTokenRepository).findByToken(TEST_TOKEN);
        verify(userRepository).findByUserId(TEST_USER_ID);
        verify(passwordEncoder).encode("newpassword");
        verify(userRepository).save(any(User.class));
        verify(passwordResetTokenRepository).save(argThat(token -> token.isUsed()));
    }

    @Test
    @DisplayName("Should fail password reset when token not found")
    void shouldFailPasswordResetWhenTokenNotFound() {
        // Given: Token not found
        when(passwordResetTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.empty());

        // When: Resetting password
        Map<String, Object> result = userPasswordService.resetPassword(TEST_TOKEN, "newpassword");

        // Then: Should return failure
        assertFalse((Boolean) result.get("success"));
        assertEquals("Invalid or expired reset token.", result.get("message"));
        verify(passwordResetTokenRepository).findByToken(TEST_TOKEN);
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should fail password reset when token is expired")
    void shouldFailPasswordResetWhenTokenExpired() {
        // Given: Expired reset token
        PasswordResetToken resetToken = createTestResetToken();
        resetToken.setUsed(false);
        resetToken.setExpiryDate(LocalDateTime.now().minusMinutes(30));
        
        when(passwordResetTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(resetToken));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(resetToken);

        // When: Resetting password
        Map<String, Object> result = userPasswordService.resetPassword(TEST_TOKEN, "newpassword");

        // Then: Should return failure and mark token as used
        assertFalse((Boolean) result.get("success"));
        assertEquals("Reset token has expired or already been used.", result.get("message"));
        verify(passwordResetTokenRepository).findByToken(TEST_TOKEN);
        verify(passwordResetTokenRepository).save(argThat(token -> token.isUsed()));
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should fail password reset when token is used")
    void shouldFailPasswordResetWhenTokenUsed() {
        // Given: Used reset token
        PasswordResetToken resetToken = createTestResetToken();
        resetToken.setUsed(true);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        
        when(passwordResetTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(resetToken));

        // When: Resetting password
        Map<String, Object> result = userPasswordService.resetPassword(TEST_TOKEN, "newpassword");

        // Then: Should return failure
        assertFalse((Boolean) result.get("success"));
        assertEquals("Reset token has expired or already been used.", result.get("message"));
        verify(passwordResetTokenRepository).findByToken(TEST_TOKEN);
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should fail password reset when user not found")
    void shouldFailPasswordResetWhenUserNotFound() {
        // Given: Valid token but user not found
        PasswordResetToken resetToken = createTestResetToken();
        resetToken.setUsed(false);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        
        when(passwordResetTokenRepository.findByToken(TEST_TOKEN)).thenReturn(Optional.of(resetToken));
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        // When: Resetting password
        Map<String, Object> result = userPasswordService.resetPassword(TEST_TOKEN, "newpassword");

        // Then: Should return failure
        assertFalse((Boolean) result.get("success"));
        assertEquals("User not found.", result.get("message"));
        verify(passwordResetTokenRepository).findByToken(TEST_TOKEN);
        verify(userRepository).findByUserId(TEST_USER_ID);
        verifyNoInteractions(passwordEncoder);
    }

    private User createTestUser() {
        User user = new User();
        user.setUserId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setPassword("encoded-password");
        user.setAccountId(TEST_ACCOUNT_ID);
        return user;
    }

    private PasswordResetToken createTestResetToken() {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(TEST_TOKEN);
        token.setUserId(TEST_USER_ID);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        token.setUsed(false);
        return token;
    }
} 