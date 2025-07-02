package com.fintrack.model.market;

import com.fintrack.model.user.Account;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fintrack.constants.finance.AssetType;

@Entity
@Table(name = "watchlist_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "symbol", "asset_type"})
})
@Data
@NoArgsConstructor
public class WatchlistData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    @JsonIgnore
    private Account account;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    // Custom setter for account to maintain the relationship
    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }
}