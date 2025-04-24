package com.fintrack.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.fintrack.constants.AssetType;

@Entity
@Table(name = "market_data_monthly", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"symbol", "date", "asset_type"})
})
public class MarketDataMonthly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    public MarketData getMarketData() {
        MarketData marketData = new MarketData();
        marketData.setSymbol(this.symbol);
        marketData.setPrice(this.price);
        marketData.setAssetType(this.assetType);
        return marketData;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }
}