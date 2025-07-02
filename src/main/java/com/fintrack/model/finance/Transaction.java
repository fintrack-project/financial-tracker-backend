package com.fintrack.model.finance;

import com.fintrack.model.user.Account;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fintrack.constants.finance.AssetType;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "account_id")
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

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "unit")
    private String unit;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 38, scale = 2, columnDefinition = "numeric(38,2) default 0")
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(nullable = false, precision = 38, scale = 2, columnDefinition = "numeric(38,2) default 0")
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
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