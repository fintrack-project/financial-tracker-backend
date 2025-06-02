package com.fintrack.model.market;

import com.fintrack.model.user.Account;
import jakarta.persistence.*;

import java.util.UUID;

import com.fintrack.constants.finance.AssetType;

@Entity
@Table(name = "watchlist_data", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "symbol", "asset_type"}))
public class WatchlistData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private Account account;

    @Column(name = "symbol", nullable = false, length = 10)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
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

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = AssetType.valueOf(assetType);
    }
}