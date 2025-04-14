package com.fintrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import java.util.*;

@Entity
@Table(name = "holdings_categories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "asset_name"})
})
public class HoldingsCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}