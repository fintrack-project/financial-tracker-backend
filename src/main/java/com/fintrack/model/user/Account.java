package com.fintrack.model.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
public class Account {
    @Id
    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId; // Primary key

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // Timestamp when the account was created

    // Pre-persist method to set default values
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}