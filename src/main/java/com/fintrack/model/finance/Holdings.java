package com.fintrack.model.finance;

import com.fintrack.model.user.Account;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

import com.fintrack.constants.finance.AssetType;

@Entity
@Table(name = "holdings", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "asset_name"}))
@Data
@NoArgsConstructor
public class Holdings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    @JsonIgnore
    private Account account;

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

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    // Custom setter for account to maintain the relationship
    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }
}