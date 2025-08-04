package com.fintrack.service.user;

import com.fintrack.model.user.Account;
import com.fintrack.model.user.User;
import com.fintrack.repository.user.AccountRepository;
import com.fintrack.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    private AccountService accountService;

    private static final String TEST_USER_ID = "testuser";
    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, userRepository);
    }

    @Test
    @DisplayName("Should get current account successfully")
    void shouldGetCurrentAccountSuccessfully() {
        // Given: Valid user exists
        User user = createTestUser();
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(user));

        // When: Getting current account
        User result = accountService.getCurrentAccount(TEST_USER_ID);

        // Then: Should return the user
        assertNotNull(result);
        assertEquals(user, result);
        assertEquals(TEST_ACCOUNT_ID, result.getAccountId());
        verify(userRepository).findByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should throw exception when user not found for current account")
    void shouldThrowExceptionWhenUserNotFoundForCurrentAccount() {
        // Given: User not found
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        // When & Then: Should throw exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> accountService.getCurrentAccount(TEST_USER_ID)
        );
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should get account ID by user ID successfully")
    void shouldGetAccountIdByUserIdSuccessfully() {
        // Given: Valid user exists
        User user = createTestUser();
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(user));

        // When: Getting account ID by user ID
        String result = accountService.getAccountIdByUserId(TEST_USER_ID);

        // Then: Should return account ID as string
        assertNotNull(result);
        assertEquals(TEST_ACCOUNT_ID.toString(), result);
        verify(userRepository).findByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return null when user not found for account ID")
    void shouldReturnNullWhenUserNotFoundForAccountId() {
        // Given: User not found
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());

        // When: Getting account ID by user ID
        String result = accountService.getAccountIdByUserId(TEST_USER_ID);

        // Then: Should return null
        assertNull(result);
        verify(userRepository).findByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should return null when user has no account ID")
    void shouldReturnNullWhenUserHasNoAccountId() {
        // Given: User exists but has no account ID
        User user = createTestUser();
        user.setAccountId(null);
        when(userRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(user));

        // When: Getting account ID by user ID
        String result = accountService.getAccountIdByUserId(TEST_USER_ID);

        // Then: Should return null
        assertNull(result);
        verify(userRepository).findByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should create account successfully")
    void shouldCreateAccountSuccessfully() {
        // Given: Account doesn't exist
        when(accountRepository.existsById(TEST_ACCOUNT_ID)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(createTestAccount());

        // When: Creating account
        accountService.createAccount(TEST_ACCOUNT_ID);

        // Then: Should save the account
        verify(accountRepository).existsById(TEST_ACCOUNT_ID);
        verify(accountRepository).save(argThat(account -> 
            TEST_ACCOUNT_ID.equals(account.getAccountId()) &&
            account.getCreatedAt() != null
        ));
    }

    @Test
    @DisplayName("Should not create account when account already exists")
    void shouldNotCreateAccountWhenAccountAlreadyExists() {
        // Given: Account already exists
        when(accountRepository.existsById(TEST_ACCOUNT_ID)).thenReturn(true);

        // When: Creating account
        accountService.createAccount(TEST_ACCOUNT_ID);

        // Then: Should check existence but not save
        verify(accountRepository).existsById(TEST_ACCOUNT_ID);
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should handle account creation with null account ID")
    void shouldHandleAccountCreationWithNullAccountId() {
        // Given: Null account ID
        UUID nullAccountId = null;

        // When & Then: Should handle gracefully (no exception)
        assertDoesNotThrow(() -> accountService.createAccount(nullAccountId));
        verify(accountRepository).existsById(null);
    }

    private User createTestUser() {
        User user = new User();
        user.setUserId(TEST_USER_ID);
        user.setAccountId(TEST_ACCOUNT_ID);
        user.setEmail("test@example.com");
        user.setPassword("encoded-password");
        return user;
    }

    private Account createTestAccount() {
        Account account = new Account();
        account.setAccountId(TEST_ACCOUNT_ID);
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
} 