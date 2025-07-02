package com.fintrack.model.finance;

import com.fintrack.model.user.Account;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "account_currencies", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"account_id", "currency"})
})
@Data
@NoArgsConstructor
public class AccountCurrency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    @JsonIgnore
    private Account account;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    // Constructor for convenience
    public AccountCurrency(UUID accountId, String currency, boolean isDefault) {
        this.accountId = accountId;
        this.currency = currency;
        this.isDefault = isDefault;
    }

    // Custom setter for account to maintain the relationship
    public void setAccount(Account account) {
        this.account = account;
        if (account != null) {
            this.accountId = account.getAccountId();
        }
    }
}