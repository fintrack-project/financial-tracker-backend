package com.fintrack.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.*;

import com.fintrack.constants.AssetType;

@Entity
@Table(name = "holdings", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "asset_name"}))
public class Holdings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "account_id", referencedColumnName = "account_id", insertable = false, updatable = false),
        @JoinColumn(name = "asset_name", referencedColumnName = "asset_name", insertable = false, updatable = false)
    })
    private Asset asset;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "total_balance", nullable = false)
    private double totalBalance;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, columnDefinition = "asset_type DEFAULT 'UNKNOWN'")
    private AssetType assetType;

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

    public Asset getAsset() {
        return asset;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
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

    public double getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(double totalBalance) {
        this.totalBalance = totalBalance;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }
}