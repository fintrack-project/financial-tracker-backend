package com.fintrack.model.user;

import java.time.LocalDateTime;
import java.util.*;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id"),
        @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @OneToOne
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    @JsonIgnore
    private Account account;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "address")
    private String address;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "failed_login_attempts", nullable = false, columnDefinition = "integer default 0")
    private int failedLoginAttempts;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked;

    @Column(name = "signup_date", nullable = false)
    private LocalDateTime signupDate;

    @Column(name = "last_activity_date")
    private LocalDateTime lastActivityDate;

    @Column(name = "storage_limit", nullable = false, columnDefinition = "integer default 0")
    private int storageLimit;

    @Column(name = "api_usage_limit", nullable = false, columnDefinition = "integer default 0")
    private int apiUsageLimit;

    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    private boolean emailVerified;

    @Column(name = "phone_verified", nullable = false, columnDefinition = "boolean default false")
    private boolean phoneVerified;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    // Custom setter for account to maintain the relationship
    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }
}