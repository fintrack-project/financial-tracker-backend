package com.fintrack.service.user;

import com.fintrack.model.user.User;
import com.fintrack.repository.user.UserRepository;
import com.fintrack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEmailService Tests")
class UserEmailServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private UserEmailService userEmailService;

    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_EMAIL = "test@example.com";
    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
    private static final String TEST_TOKEN = "test-verification-token";
    private static final String TEST_BASE_URL = "http://localhost:8080";
    private static final String TEST_FROM_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        userEmailService = new UserEmailService(
            userRepository,
            jwtService,
            passwordEncoder,
            mailSender
        );
        
        // Set the @Value fields manually for testing
        ReflectionTestUtils.setField(userEmailService, "baseUrl", TEST_BASE_URL);
        ReflectionTestUtils.setField(userEmailService, "fromEmail", TEST_FROM_EMAIL);
    }

    @Test
    @DisplayName("Should send verification email successfully")
    void shouldSendVerificationEmailSuccessfully() throws MessagingException {
        // Given: Valid user and email setup
        User user = createTestUser();
        when(jwtService.generateVerificationToken(user.getUserId().toString())).thenReturn(TEST_TOKEN);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When: Sending verification email
        userEmailService.sendVerificationEmail(TEST_EMAIL, user);

        // Then: Should send email successfully
        verify(jwtService).generateVerificationToken(user.getUserId().toString());
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should throw exception when email sending fails")
    void shouldThrowExceptionWhenEmailSendingFails() throws MessagingException {
        // Given: Email sending fails
        User user = createTestUser();
        when(jwtService.generateVerificationToken(user.getUserId().toString())).thenReturn(TEST_TOKEN);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Failed to send")).when(mailSender).send(any(MimeMessage.class));

        // When & Then: Should throw exception
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userEmailService.sendVerificationEmail(TEST_EMAIL, user)
        );
        assertEquals("Failed to send", exception.getMessage());
        verify(jwtService).generateVerificationToken(user.getUserId().toString());
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should verify email successfully")
    void shouldVerifyEmailSuccessfully() {
        // Given: Valid token and unverified user
        User user = createTestUser();
        user.setEmailVerified(false);
        
        when(jwtService.decodeToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When: Verifying email
        String result = userEmailService.verifyEmail(TEST_TOKEN);

        // Then: Should verify successfully
        assertEquals("Email verified successfully.", result);
        verify(jwtService).decodeToken(TEST_TOKEN);
        verify(userRepository).findByUserId(TEST_USER_ID);
        verify(userRepository).save(argThat(savedUser -> savedUser.isEmailVerified()));
    }

    @Test
    @DisplayName("Should return invalid token when token is invalid")
    void shouldReturnInvalidTokenWhenTokenIsInvalid() {
        // Given: Invalid token
        when(jwtService.decodeToken(TEST_TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        // When: Verifying email
        String result = userEmailService.verifyEmail(TEST_TOKEN);

        // Then: Should return invalid token message
        assertEquals("Invalid or expired token.", result);
        verify(jwtService).decodeToken(TEST_TOKEN);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Should return invalid token when user not found")
    void shouldReturnInvalidTokenWhenUserNotFound() {
        // Given: Valid token but user not found
        when(jwtService.decodeToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        // When: Verifying email
        String result = userEmailService.verifyEmail(TEST_TOKEN);

        // Then: Should return invalid token message
        assertEquals("Invalid token.", result);
        verify(jwtService).decodeToken(TEST_TOKEN);
        verify(userRepository).findByUserId(TEST_USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Should return already verified when email is already verified")
    void shouldReturnAlreadyVerifiedWhenEmailIsAlreadyVerified() {
        // Given: Valid token and already verified user
        User user = createTestUser();
        user.setEmailVerified(true);
        
        when(jwtService.decodeToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(user));

        // When: Verifying email
        String result = userEmailService.verifyEmail(TEST_TOKEN);

        // Then: Should return already verified message
        assertEquals("Email already verified.", result);
        verify(jwtService).decodeToken(TEST_TOKEN);
        verify(userRepository).findByUserId(TEST_USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Should check email verification status successfully")
    void shouldCheckEmailVerificationStatusSuccessfully() {
        // Given: User exists and email is verified
        User user = createTestUser();
        user.setEmailVerified(true);
        when(userRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));

        // When: Checking email verification status
        boolean result = userEmailService.isEmailVerified(TEST_ACCOUNT_ID);

        // Then: Should return true
        assertTrue(result);
        verify(userRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should return false when email is not verified")
    void shouldReturnFalseWhenEmailIsNotVerified() {
        // Given: User exists but email is not verified
        User user = createTestUser();
        user.setEmailVerified(false);
        when(userRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));

        // When: Checking email verification status
        boolean result = userEmailService.isEmailVerified(TEST_ACCOUNT_ID);

        // Then: Should return false
        assertFalse(result);
        verify(userRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should throw exception when user not found for email verification check")
    void shouldThrowExceptionWhenUserNotFoundForEmailVerificationCheck() {
        // Given: User not found
        when(userRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userEmailService.isEmailVerified(TEST_ACCOUNT_ID)
        );
        assertEquals("User not found with accountId: " + TEST_ACCOUNT_ID, exception.getMessage());
        verify(userRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should send email verification successfully")
    void shouldSendEmailVerificationSuccessfully() throws MessagingException {
        // Given: Valid user and email verification setup
        User user = createTestUser();
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(jwtService.generateVerificationToken(user.getUserId().toString())).thenReturn(TEST_TOKEN);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When: Sending email verification
        userEmailService.sendEmailVerification(TEST_ACCOUNT_ID, TEST_EMAIL);

        // Then: Should send verification email
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verify(jwtService).generateVerificationToken(user.getUserId().toString());
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found for email verification")
    void shouldThrowExceptionWhenUserNotFoundForEmailVerification() {
        // Given: User not found
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userEmailService.sendEmailVerification(TEST_ACCOUNT_ID, TEST_EMAIL)
        );
        assertEquals("User not found with accountId: " + TEST_ACCOUNT_ID, exception.getMessage());
        verify(userRepository).findById(TEST_ACCOUNT_ID);
        verifyNoInteractions(jwtService, mailSender);
    }

    @Test
    @DisplayName("Should handle email template loading failure gracefully")
    void shouldHandleEmailTemplateLoadingFailureGracefully() throws MessagingException {
        // Given: Valid user but template loading fails
        User user = createTestUser();
        when(jwtService.generateVerificationToken(user.getUserId().toString())).thenReturn(TEST_TOKEN);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        // When: Sending verification email (template loading will fail in test environment)
        userEmailService.sendVerificationEmail(TEST_EMAIL, user);

        // Then: Should still send email with fallback template
        verify(jwtService).generateVerificationToken(user.getUserId().toString());
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(any(MimeMessage.class));
    }

    private User createTestUser() {
        User user = new User();
        user.setUserId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setAccountId(TEST_ACCOUNT_ID);
        user.setEmailVerified(false);
        return user;
    }
} 