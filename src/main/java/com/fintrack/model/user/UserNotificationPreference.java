package com.fintrack.model.user;

import jakarta.persistence.*;

import java.util.*;

import com.fintrack.constants.user.UserNotificationType;

@Entity
@Table(name = "user_notification_preferences")
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private UserNotificationType notificationType;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UserNotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(UserNotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }
}