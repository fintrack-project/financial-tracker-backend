package com.fintrack.model.finance;

import com.fintrack.model.user.Account;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fintrack.constants.finance.AssetType;

@Entity
@Table(name = "holdings_monthly", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "asset_name", "date"})
})
@Data
@NoArgsConstructor
public class HoldingsMonthly {

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
    @JsonIgnore
    private Asset asset;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "asset_name", nullable = false)
    private String assetName;

    @Column(name = "total_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalBalance;

    @Column(name = "unit", nullable = false, length = 50)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;
    
    @Column(name = "symbol", nullable = false)
    private String symbol;

    public Holdings getHoldings() {
        Holdings holdings = new Holdings();
        holdings.setAccountId(getAccountId());
        holdings.setAssetName(getAssetName());
        holdings.setTotalBalance(getTotalBalance().doubleValue());
        holdings.setUnit(getUnit());
        holdings.setSymbol(getSymbol());
        holdings.setAssetType(getAssetType());
        return holdings;
    }

    // Custom setter for account to maintain the relationship
    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }
}