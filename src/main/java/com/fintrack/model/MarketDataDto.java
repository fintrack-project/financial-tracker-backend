package com.fintrack.model;

import com.fintrack.constants.AssetType;
import java.math.BigDecimal;

public class MarketDataDto {

    private String symbol;
    private BigDecimal price;
    private AssetType assetType;

    // Constructor
    public MarketDataDto(String symbol, BigDecimal price, AssetType assetType) {
        this.symbol = symbol;
        this.price = price;
        this.assetType = assetType;
    }

    public MarketDataDto(MarketData marketData) {
        this.symbol = marketData.getSymbol();
        this.price = marketData.getPrice();
        this.assetType = marketData.getAssetType();
    }

    public MarketDataDto(MarketDataMonthly marketDataMonthly) {
        this.symbol = marketDataMonthly.getSymbol();
        this.price = marketDataMonthly.getPrice();
        this.assetType = marketDataMonthly.getAssetType();
    }

    // Getters and Setters
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

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    @Override
    public String toString() {
        return "MarketDataDto{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", assetType=" + assetType +
                '}';
    }
}