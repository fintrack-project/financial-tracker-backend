package com.fintrack.repository.user;

import com.fintrack.constants.user.UserNotificationType;
import com.fintrack.model.user.UserNotificationPreference;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {
    List<UserNotificationPreference> findByAccountId(UUID accountId);
    Optional<UserNotificationPreference> findByAccountIdAndNotificationType(UUID accountId, UserNotificationType notificationType);
}