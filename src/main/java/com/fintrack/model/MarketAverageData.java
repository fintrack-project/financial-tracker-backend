package com.fintrack.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_average_data", uniqueConstraints = @UniqueConstraint(columnNames = "symbol"))
public class MarketAverageData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symbol", nullable = false, length = 255, unique = true)
    private String symbol;

    @Column(name = "price", nullable = false, length = 255)
    private String price;

    @Column(name = "price_change")
    private Double priceChange;

    @Column(name = "percent_change", length = 255)
    private String percentChange;

    @Column(name = "price_high")
    private Double priceHigh;

    @Column(name = "price_low")
    private Double priceLow;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public Double getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(Double priceChange) {
        this.priceChange = priceChange;
    }

    public String getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(String percentChange) {
        this.percentChange = percentChange;
    }

    public Double getPriceHigh() {
        return priceHigh;
    }

    public void setPriceHigh(Double priceHigh) {
        this.priceHigh = priceHigh;
    }

    public Double getPriceLow() {
        return priceLow;
    }

    public void setPriceLow(Double priceLow) {
        this.priceLow = priceLow;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}