package com.fintrack.model.finance;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "account_currencies", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "currency"})
})
public class AccountCurrency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    // Constructors
    public AccountCurrency() {}

    public AccountCurrency(UUID accountId, String currency, boolean isDefault) {
        this.accountId = accountId;
        this.currency = currency;
        this.isDefault = isDefault;
    }

    // Getters and Setters
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}