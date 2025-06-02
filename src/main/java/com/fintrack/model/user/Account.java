package com.fintrack.model.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId; // Primary key

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // Timestamp when the account was created

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL)
    private User user;

    // Getters and Setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Pre-persist method to set default values
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}