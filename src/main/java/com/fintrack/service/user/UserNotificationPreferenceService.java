package com.fintrack.service.user;

import com.fintrack.constants.user.UserNotificationType;
import com.fintrack.model.user.UserNotificationPreference;
import com.fintrack.repository.user.UserNotificationPreferenceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserNotificationPreferenceService {

    @Autowired
    private UserNotificationPreferenceRepository repository;

    public List<UserNotificationPreference> getNotificationPreferencesByAccountId(UUID accountId) {
        return repository.findByAccountId(accountId);
    }

    public void updateNotificationPreference(UUID accountId, UserNotificationType notificationType, boolean isEnabled) {
        // Find the existing preference
        Optional<UserNotificationPreference> existingPreference = repository.findByAccountIdAndNotificationType(accountId, notificationType);

        if (existingPreference.isPresent()) {
            // Update the existing preference
            UserNotificationPreference preference = existingPreference.get();
            preference.setEnabled(isEnabled);
            repository.save(preference);
        } else {
            // Create a new preference if it doesn't exist
            UserNotificationPreference newPreference = new UserNotificationPreference();
            newPreference.setAccountId(accountId);
            newPreference.setNotificationType(notificationType);
            newPreference.setEnabled(isEnabled);
            repository.save(newPreference);
        }
    }
}