package com.fintrack.service.user;

import com.fintrack.constants.user.UserNotificationType;
import com.fintrack.model.user.UserNotificationPreference;
import com.fintrack.repository.user.UserNotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserNotificationPreferenceService Tests")
class UserNotificationPreferenceServiceTest {

    @Mock
    private UserNotificationPreferenceRepository repository;

    private UserNotificationPreferenceService userNotificationPreferenceService;

    private static final UUID TEST_ACCOUNT_ID = UUID.randomUUID();
    private static final UserNotificationType TEST_NOTIFICATION_TYPE = UserNotificationType.EMAIL;
    private static final UserNotificationType TEST_NOTIFICATION_TYPE_2 = UserNotificationType.SMS;

    @BeforeEach
    void setUp() {
        userNotificationPreferenceService = new UserNotificationPreferenceService();
        // Use reflection to set the repository field since it's @Autowired
        org.springframework.test.util.ReflectionTestUtils.setField(
            userNotificationPreferenceService, "repository", repository
        );
    }

    @Test
    @DisplayName("Should get notification preferences successfully")
    void shouldGetNotificationPreferencesSuccessfully() {
        // Given: User has notification preferences
        UserNotificationPreference preference1 = createTestPreference(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE, true);
        UserNotificationPreference preference2 = createTestPreference(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE_2, false);
        List<UserNotificationPreference> expectedPreferences = Arrays.asList(preference1, preference2);
        
        when(repository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(expectedPreferences);

        // When: Getting notification preferences
        List<UserNotificationPreference> result = userNotificationPreferenceService.getNotificationPreferencesByAccountId(TEST_ACCOUNT_ID);

        // Then: Should return the preferences
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedPreferences, result);
        verify(repository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should return empty list when no preferences exist")
    void shouldReturnEmptyListWhenNoPreferencesExist() {
        // Given: No notification preferences exist
        when(repository.findByAccountId(TEST_ACCOUNT_ID)).thenReturn(Arrays.asList());

        // When: Getting notification preferences
        List<UserNotificationPreference> result = userNotificationPreferenceService.getNotificationPreferencesByAccountId(TEST_ACCOUNT_ID);

        // Then: Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository).findByAccountId(TEST_ACCOUNT_ID);
    }

    @Test
    @DisplayName("Should update existing notification preference successfully")
    void shouldUpdateExistingNotificationPreferenceSuccessfully() {
        // Given: Existing preference found
        UserNotificationPreference existingPreference = createTestPreference(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE, false);
        when(repository.findByAccountIdAndNotificationType(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE))
            .thenReturn(Optional.of(existingPreference));
        when(repository.save(any(UserNotificationPreference.class))).thenReturn(existingPreference);

        // When: Updating notification preference
        userNotificationPreferenceService.updateNotificationPreference(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE, true);

        // Then: Should update the existing preference
        verify(repository).findByAccountIdAndNotificationType(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE);
        verify(repository).save(argThat(preference -> 
            preference.getAccountId().equals(TEST_ACCOUNT_ID) &&
            preference.getNotificationType().equals(TEST_NOTIFICATION_TYPE) &&
            preference.isEnabled()
        ));
    }

    @Test
    @DisplayName("Should create new notification preference when none exists")
    void shouldCreateNewNotificationPreferenceWhenNoneExists() {
        // Given: No existing preference found
        when(repository.findByAccountIdAndNotificationType(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE))
            .thenReturn(Optional.empty());
        when(repository.save(any(UserNotificationPreference.class))).thenReturn(createTestPreference(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE, true));

        // When: Updating notification preference
        userNotificationPreferenceService.updateNotificationPreference(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE, true);

        // Then: Should create new preference
        verify(repository).findByAccountIdAndNotificationType(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE);
        verify(repository).save(argThat(preference -> 
            preference.getAccountId().equals(TEST_ACCOUNT_ID) &&
            preference.getNotificationType().equals(TEST_NOTIFICATION_TYPE) &&
            preference.isEnabled()
        ));
    }

    @Test
    @DisplayName("Should disable notification preference successfully")
    void shouldDisableNotificationPreferenceSuccessfully() {
        // Given: Existing enabled preference
        UserNotificationPreference existingPreference = createTestPreference(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE, true);
        when(repository.findByAccountIdAndNotificationType(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE))
            .thenReturn(Optional.of(existingPreference));
        when(repository.save(any(UserNotificationPreference.class))).thenReturn(existingPreference);

        // When: Disabling notification preference
        userNotificationPreferenceService.updateNotificationPreference(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE, false);

        // Then: Should disable the preference
        verify(repository).findByAccountIdAndNotificationType(TEST_ACCOUNT_ID, TEST_NOTIFICATION_TYPE);
        verify(repository).save(argThat(preference -> 
            preference.getAccountId().equals(TEST_ACCOUNT_ID) &&
            preference.getNotificationType().equals(TEST_NOTIFICATION_TYPE) &&
            !preference.isEnabled()
        ));
    }

    @Test
    @DisplayName("Should handle null notification type gracefully")
    void shouldHandleNullNotificationTypeGracefully() {
        // Given: Null notification type
        when(repository.findByAccountIdAndNotificationType(TEST_ACCOUNT_ID, null))
            .thenReturn(Optional.empty());
        when(repository.save(any(UserNotificationPreference.class)))
            .thenReturn(createTestPreference(TEST_ACCOUNT_ID, null, true));

        // When: Updating with null notification type
        userNotificationPreferenceService.updateNotificationPreference(TEST_ACCOUNT_ID, null, true);

        // Then: Should handle gracefully
        verify(repository).findByAccountIdAndNotificationType(TEST_ACCOUNT_ID, null);
        verify(repository).save(any(UserNotificationPreference.class));
    }

    private UserNotificationPreference createTestPreference(UUID accountId, UserNotificationType type, boolean enabled) {
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setAccountId(accountId);
        preference.setNotificationType(type);
        preference.setEnabled(enabled);
        return preference;
    }
} 