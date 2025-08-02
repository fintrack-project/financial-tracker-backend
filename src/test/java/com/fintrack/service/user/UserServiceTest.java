package com.fintrack.service.user;

import com.fintrack.model.user.User;
import com.fintrack.repository.user.UserRepository;
import com.fintrack.security.JwtService;
import com.fintrack.service.subscription.UserSubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserEmailService userEmailService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserSubscriptionService userSubscriptionService;

    @Mock
    private AccountService accountService;

    private UserService userService;

    private static final String TEST_USER_ID = "testuser";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_EMAIL = "test@example.com";
    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userService = new UserService(
            userRepository,
            userEmailService,
            passwordEncoder,
            jwtService,
            userSubscriptionService,
            accountService
        );
    }

    @Test
    @DisplayName("Should authenticate user successfully with valid credentials")
    void shouldAuthenticateUserSuccessfully() {
        // Given: Valid user credentials
        User user = createTestUser();
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(true);
        when(jwtService.generateVerificationToken(user.getUserId().toString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user.getUserId().toString())).thenReturn("refresh-token");

        // When: Authenticating user
        Map<String, Object> result = userService.authenticateAndGenerateToken(TEST_USER_ID, TEST_PASSWORD);

        // Then: Should return authentication tokens and user info
        assertNotNull(result);
        assertEquals("access-token", result.get("token"));
        assertEquals("refresh-token", result.get("refreshToken"));
        assertEquals(user.getUserId(), result.get("userId"));
        assertEquals(user.getAccountId(), result.get("accountId"));
        
        verify(userRepository).save(user);
        verify(userRepository).findByUserId(TEST_USER_ID);
        verify(passwordEncoder).matches(TEST_PASSWORD, user.getPassword());
        verify(jwtService).generateVerificationToken(user.getUserId().toString());
        verify(jwtService).generateRefreshToken(user.getUserId().toString());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given: User not found
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticateAndGenerateToken(TEST_USER_ID, TEST_PASSWORD)
        );
        assertEquals("Invalid userId or password.", exception.getMessage());
        
        verify(userRepository).findByUserId(TEST_USER_ID);
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Should throw exception when password is invalid")
    void shouldThrowExceptionWhenPasswordInvalid() {
        // Given: Valid user but invalid password
        User user = createTestUser();
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(false);

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticateAndGenerateToken(TEST_USER_ID, TEST_PASSWORD)
        );
        assertEquals("Invalid userId or password.", exception.getMessage());
        
        verify(userRepository).save(user);
        verify(userRepository).findByUserId(TEST_USER_ID);
        verify(passwordEncoder).matches(TEST_PASSWORD, user.getPassword());
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should lock account after maximum failed attempts")
    void shouldLockAccountAfterMaxFailedAttempts() {
        // Given: User with maximum failed attempts
        User user = createTestUser();
        user.setFailedLoginAttempts(4); // One attempt away from lock
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(false);

        // When: Attempting to authenticate with wrong password
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticateAndGenerateToken(TEST_USER_ID, TEST_PASSWORD)
        );

        // Then: Should lock account
        assertEquals("Invalid userId or password.", exception.getMessage());
        verify(userRepository).save(argThat(savedUser -> 
            savedUser.isAccountLocked() && savedUser.getFailedLoginAttempts() == 5
        ));
    }

    @Test
    @DisplayName("Should unlock account when blocking time is over")
    void shouldUnlockAccountWhenBlockingTimeOver() {
        // Given: Locked user with expired blocking time
        User user = createTestUser();
        user.setAccountLocked(true);
        user.setLastActivityDate(LocalDateTime.now().minusMinutes(2)); // More than 1 minute ago
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(TEST_PASSWORD, user.getPassword())).thenReturn(true);
        when(jwtService.generateVerificationToken(user.getUserId().toString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user.getUserId().toString())).thenReturn("refresh-token");

        // When: Authenticating user
        Map<String, Object> result = userService.authenticateAndGenerateToken(TEST_USER_ID, TEST_PASSWORD);

        // Then: Should unlock account and authenticate successfully
        assertNotNull(result);
        verify(userRepository).save(argThat(savedUser -> 
            !savedUser.isAccountLocked() && savedUser.getFailedLoginAttempts() == 0
        ));
    }

    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() {
        // Given: New user registration data
        User newUser = createTestUser();
        when(userRepository.findByUserId(newUser.getUserId())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        doNothing().when(accountService).createAccount(any(UUID.class));
        doNothing().when(userSubscriptionService).createFreeSubscription(any(UUID.class), eq("Free"));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        // When: Registering user
        String result = userService.registerUser(newUser);

        // Then: Should register successfully
        assertEquals("User registered successfully.", result);
        verify(userRepository).findByUserId(newUser.getUserId());
        verify(userRepository).findByEmail(newUser.getEmail());
        verify(accountService).createAccount(any(UUID.class));
        verify(userSubscriptionService).createFreeSubscription(any(UUID.class), eq("Free"));
        verify(passwordEncoder).encode(newUser.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user ID already exists")
    void shouldThrowExceptionWhenUserIdExists() {
        // Given: User ID already exists
        User newUser = createTestUser();
        when(userRepository.findByUserId(newUser.getUserId())).thenReturn(Optional.of(newUser));

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(newUser)
        );
        assertEquals("User ID already exists.", exception.getMessage());
        
        verify(userRepository).findByUserId(newUser.getUserId());
        verifyNoInteractions(accountService, userSubscriptionService);
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailExists() {
        // Given: Email already exists
        User newUser = createTestUser();
        when(userRepository.findByUserId(newUser.getUserId())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(newUser.getEmail())).thenReturn(Optional.of(newUser));

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(newUser)
        );
        assertEquals("Email already exists.", exception.getMessage());
        
        verify(userRepository).findByUserId(newUser.getUserId());
        verify(userRepository).findByEmail(newUser.getEmail());
        verifyNoInteractions(accountService, userSubscriptionService);
    }

    @Test
    @DisplayName("Should fetch user details successfully")
    void shouldFetchUserDetailsSuccessfully() {
        // Given: User exists
        User user = createTestUser();
        when(userRepository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));

        // When: Fetching user details
        Optional<User> result = userService.fetchUserDetails(TEST_ACCOUNT_ID);

        // Then: Should return user details
        assertTrue(result.isPresent());
        assertEquals(user, result.get());
        verify(userRepository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should update user phone successfully")
    void shouldUpdateUserPhoneSuccessfully() {
        // Given: User exists
        User user = createTestUser();
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When: Updating user phone
        userService.updateUserPhone(TEST_ACCOUNT_ID, "1234567890", "+1");

        // Then: Should update phone and reset verification
        verify(userRepository).save(argThat(savedUser -> 
            "1234567890".equals(savedUser.getPhone()) &&
            "+1".equals(savedUser.getCountryCode()) &&
            !savedUser.isPhoneVerified()
        ));
    }

    @Test
    @DisplayName("Should throw exception when updating phone for non-existent user")
    void shouldThrowExceptionWhenUpdatingPhoneForNonExistentUser() {
        // Given: User doesn't exist
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateUserPhone(TEST_ACCOUNT_ID, "1234567890", "+1")
        );
        assertEquals("User not found with accountId: " + TEST_ACCOUNT_ID, exception.getMessage());
    }

    @Test
    @DisplayName("Should set phone verified successfully")
    void shouldSetPhoneVerifiedSuccessfully() {
        // Given: User exists
        User user = createTestUser();
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When: Setting phone as verified
        boolean result = userService.setPhoneVerified(TEST_ACCOUNT_ID);

        // Then: Should return true and update verification status
        assertTrue(result);
        verify(userRepository).save(argThat(savedUser -> savedUser.isPhoneVerified()));
    }

    @Test
    @DisplayName("Should update user address successfully")
    void shouldUpdateUserAddressSuccessfully() {
        // Given: User exists
        User user = createTestUser();
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When: Updating user address
        userService.updateUserAddress(TEST_ACCOUNT_ID, "123 Main St");

        // Then: Should update address
        verify(userRepository).save(argThat(savedUser -> 
            "123 Main St".equals(savedUser.getAddress())
        ));
    }

    @Test
    @DisplayName("Should update user email successfully")
    void shouldUpdateUserEmailSuccessfully() {
        // Given: User exists
        User user = createTestUser();
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When: Updating user email
        userService.updateUserEmail(TEST_ACCOUNT_ID, "newemail@example.com");

        // Then: Should update email and reset verification
        verify(userRepository).save(argThat(savedUser -> 
            "newemail@example.com".equals(savedUser.getEmail()) &&
            !savedUser.isEmailVerified()
        ));
    }

    @Test
    @DisplayName("Should update user password successfully")
    void shouldUpdateUserPasswordSuccessfully() {
        // Given: User exists
        User user = createTestUser();
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("new-encoded-password");

        // When: Updating user password
        userService.updateUserPassword(TEST_ACCOUNT_ID, "newpassword");

        // Then: Should update password
        verify(passwordEncoder).encode("newpassword");
        verify(userRepository).save(argThat(savedUser -> 
            "new-encoded-password".equals(savedUser.getPassword())
        ));
    }

    @Test
    @DisplayName("Should update user two-factor status successfully")
    void shouldUpdateUserTwoFactorStatusSuccessfully() {
        // Given: User exists
        User user = createTestUser();
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When: Enabling two-factor authentication
        userService.updateUserTwoFactorStatus(TEST_ACCOUNT_ID, true);

        // Then: Should update two-factor status
        verify(userRepository).save(argThat(savedUser -> savedUser.isTwoFactorEnabled()));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent user")
    void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        // Given: User doesn't exist
        when(userRepository.findById(TEST_ACCOUNT_ID)).thenReturn(Optional.empty());

        // When & Then: Should throw exception for various update methods
        assertThrows(IllegalArgumentException.class, 
            () -> userService.updateUserPhone(TEST_ACCOUNT_ID, "1234567890", "+1"));
        assertThrows(IllegalArgumentException.class, 
            () -> userService.setPhoneVerified(TEST_ACCOUNT_ID));
        assertThrows(IllegalArgumentException.class, 
            () -> userService.updateUserAddress(TEST_ACCOUNT_ID, "123 Main St"));
        assertThrows(IllegalArgumentException.class, 
            () -> userService.updateUserEmail(TEST_ACCOUNT_ID, "newemail@example.com"));
        assertThrows(IllegalArgumentException.class, 
            () -> userService.updateUserPassword(TEST_ACCOUNT_ID, "newpassword"));
        assertThrows(IllegalArgumentException.class, 
            () -> userService.updateUserTwoFactorStatus(TEST_ACCOUNT_ID, true));
    }

    private User createTestUser() {
        User user = new User();
        user.setUserId(TEST_USER_ID);
        user.setEmail(TEST_EMAIL);
        user.setPassword("encoded-password");
        user.setAccountId(TEST_ACCOUNT_ID);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setEmailVerified(true);
        user.setPhoneVerified(false);
        user.setTwoFactorEnabled(false);
        return user;
    }
} 