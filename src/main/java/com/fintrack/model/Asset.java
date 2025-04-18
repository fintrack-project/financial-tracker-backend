package com.fintrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "asset", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "asset_name"}))
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private String assetName;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String unit;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}