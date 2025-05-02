package com.fintrack.repository;

import com.fintrack.constants.UserNotificationType;
import com.fintrack.model.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {
    List<UserNotificationPreference> findByAccountId(UUID accountId);
    Optional<UserNotificationPreference> findByAccountIdAndNotificationType(UUID accountId, UserNotificationType notificationType);
}