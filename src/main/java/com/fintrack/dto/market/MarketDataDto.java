package com.fintrack.dto.market;

import com.fintrack.constants.finance.AssetType;
import com.fintrack.model.market.MarketData;
import com.fintrack.model.market.MarketDataMonthly;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataDto {

    private String symbol;
    private BigDecimal price;
    private AssetType assetType;

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

    @Override
    public String toString() {
        return "MarketDataDto{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                ", assetType=" + assetType +
                '}';
    }
}