package com.fintrack.model;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "holdings")
public class HoldingDto {
    @Id
    @GeneratedValue
    @Column(name = "asset_name", nullable = false, unique = true, updatable = false)
    private String assetName;
    @Column(name = "total_balance", nullable = false)
    private double totalBalance;
    @Column(name = "unit", nullable = false)
    private String unit;

    // Getters and Setters
    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public double getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(double totalBalance) {
        this.totalBalance = totalBalance;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}