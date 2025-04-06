package com.fintrack.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String assetName;

    @Column(nullable = false)
    private BigDecimal credit;

    @Column(nullable = false)
    private BigDecimal debit;

    @Column(nullable = false)
    private BigDecimal totalBalanceBefore;

    @Column(nullable = false)
    private BigDecimal totalBalanceAfter;

    @Column(nullable = false)
    private String unit;

    // Getters and Setters
    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getTotalBalanceBefore() {
        return totalBalanceBefore;
    }

    public void setTotalBalanceBefore(BigDecimal totalBalanceBefore) {
        this.totalBalanceBefore = totalBalanceBefore;
    }

    public BigDecimal getTotalBalanceAfter() {
        return totalBalanceAfter;
    }

    public void setTotalBalanceAfter(BigDecimal totalBalanceAfter) {
        this.totalBalanceAfter = totalBalanceAfter;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}