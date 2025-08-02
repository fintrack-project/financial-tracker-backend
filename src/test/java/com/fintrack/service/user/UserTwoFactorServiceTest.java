package com.fintrack.service.user;

import com.fintrack.model.user.User;
import com.fintrack.repository.user.UserRepository;
import com.fintrack.security.TwoFactorService;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserTwoFactorService Tests")
class UserTwoFactorServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TwoFactorService twoFactorService;

    @Mock
    private GoogleAuthenticatorKey googleAuthenticatorKey;

    private UserTwoFactorService userTwoFactorService;

    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_SECRET = "test-secret-key";
    private static final String TEST_QR_CODE = "test-qr-code";
    private static final int TEST_OTP = 123456;

    @BeforeEach
    void setUp() {
        userTwoFactorService = new UserTwoFactorService(userRepository, twoFactorService);
    }

    @Test
    @DisplayName("Should setup two factor authentication successfully")
    void shouldSetupTwoFactorAuthenticationSuccessfully() {
        // Given: Valid user and 2FA setup
        User user = createTestUser();
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(twoFactorService.generateSecret()).thenReturn(googleAuthenticatorKey);
        when(googleAuthenticatorKey.getKey()).thenReturn(TEST_SECRET);
        when(twoFactorService.generateQRCode(user.getEmail(), googleAuthenticatorKey)).thenReturn(TEST_QR_CODE);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When: Setting up 2FA
        Map<String, String> result = userTwoFactorService.setupTwoFactorAuthentication(TEST_ACCOUNT_ID);

        // Then: Should return secret and QR code
        assertNotNull(result);
        assertEquals(TEST_SECRET, result.get("secret"));
        assertEquals(TEST_QR_CODE, result.get("qrCode"));
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verify(twoFactorService).generateSecret();
        verify(googleAuthenticatorKey).getKey();
        verify(twoFactorService).generateQRCode(user.getEmail(), googleAuthenticatorKey);
        verify(userRepository).save(argThat(savedUser -> TEST_SECRET.equals(savedUser.getTwoFactorSecret())));
    }

    @Test
    @DisplayName("Should throw exception when user not found for 2FA setup")
    void shouldThrowExceptionWhenUserNotFoundFor2FASetup() {
        // Given: User not found
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userTwoFactorService.setupTwoFactorAuthentication(TEST_ACCOUNT_ID)
        );
        assertEquals("User not found.", exception.getMessage());
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verifyNoInteractions(twoFactorService);
    }

    @Test
    @DisplayName("Should verify two factor authentication successfully")
    void shouldVerifyTwoFactorAuthenticationSuccessfully() {
        // Given: Valid user and valid OTP
        User user = createTestUser();
        user.setTwoFactorSecret(TEST_SECRET);
        user.setTwoFactorEnabled(false);
        
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(twoFactorService.verifyOTP(TEST_SECRET, TEST_OTP)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When: Verifying 2FA
        Map<String, Object> result = userTwoFactorService.verifyTwoFactorAuthentication(TEST_ACCOUNT_ID, TEST_OTP);

        // Then: Should return success
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals("OTP verified successfully.", result.get("message"));
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verify(twoFactorService).verifyOTP(TEST_SECRET, TEST_OTP);
        verify(userRepository).save(argThat(savedUser -> savedUser.isTwoFactorEnabled()));
    }

    @Test
    @DisplayName("Should return failure when OTP is invalid")
    void shouldReturnFailureWhenOTPIsInvalid() {
        // Given: Valid user but invalid OTP
        User user = createTestUser();
        user.setTwoFactorSecret(TEST_SECRET);
        user.setTwoFactorEnabled(false);
        
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(twoFactorService.verifyOTP(TEST_SECRET, TEST_OTP)).thenReturn(false);

        // When: Verifying 2FA
        Map<String, Object> result = userTwoFactorService.verifyTwoFactorAuthentication(TEST_ACCOUNT_ID, TEST_OTP);

        // Then: Should return failure
        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("Invalid OTP.", result.get("message"));
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verify(twoFactorService).verifyOTP(TEST_SECRET, TEST_OTP);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Should throw exception when user not found for 2FA verification")
    void shouldThrowExceptionWhenUserNotFoundFor2FAVerification() {
        // Given: User not found
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userTwoFactorService.verifyTwoFactorAuthentication(TEST_ACCOUNT_ID, TEST_OTP)
        );
        assertEquals("User not found.", exception.getMessage());
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verifyNoInteractions(twoFactorService);
    }

    @Test
    @DisplayName("Should handle user with no 2FA secret")
    void shouldHandleUserWithNo2FASecret() {
        // Given: User without 2FA secret
        User user = createTestUser();
        user.setTwoFactorSecret(null);
        
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(twoFactorService.verifyOTP(null, TEST_OTP)).thenReturn(false);

        // When: Verifying 2FA
        Map<String, Object> result = userTwoFactorService.verifyTwoFactorAuthentication(TEST_ACCOUNT_ID, TEST_OTP);

        // Then: Should return failure
        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("Invalid OTP.", result.get("message"));
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verify(twoFactorService).verifyOTP(null, TEST_OTP);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Should handle already enabled 2FA user")
    void shouldHandleAlreadyEnabled2FAUser() {
        // Given: User with already enabled 2FA
        User user = createTestUser();
        user.setTwoFactorSecret(TEST_SECRET);
        user.setTwoFactorEnabled(true);
        
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(twoFactorService.verifyOTP(TEST_SECRET, TEST_OTP)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When: Verifying 2FA
        Map<String, Object> result = userTwoFactorService.verifyTwoFactorAuthentication(TEST_ACCOUNT_ID, TEST_OTP);

        // Then: Should still return success (re-verification is allowed)
        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertEquals("OTP verified successfully.", result.get("message"));
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verify(twoFactorService).verifyOTP(TEST_SECRET, TEST_OTP);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle zero OTP value")
    void shouldHandleZeroOTPValue() {
        // Given: Valid user but zero OTP
        User user = createTestUser();
        user.setTwoFactorSecret(TEST_SECRET);
        
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(twoFactorService.verifyOTP(TEST_SECRET, 0)).thenReturn(false);

        // When: Verifying 2FA with zero OTP
        Map<String, Object> result = userTwoFactorService.verifyTwoFactorAuthentication(TEST_ACCOUNT_ID, 0);

        // Then: Should return failure
        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("Invalid OTP.", result.get("message"));
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verify(twoFactorService).verifyOTP(TEST_SECRET, 0);
        verifyNoMoreInteractions(userRepository);
    }

    private User createTestUser() {
        User user = new User();
        user.setAccountId(TEST_ACCOUNT_ID);
        user.setEmail(TEST_EMAIL);
        user.setUserId("testuser");
        user.setTwoFactorEnabled(false);
        return user;
    }
} 