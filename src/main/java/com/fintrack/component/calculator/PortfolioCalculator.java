package com.fintrack.component.calculator;

import com.fintrack.constants.finance.AssetType;
import com.fintrack.dto.market.MarketDataDto;
import com.fintrack.model.finance.Holdings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class PortfolioCalculator {

    private static final Logger logger = LoggerFactory.getLogger(PortfolioCalculator.class);

    private final UUID accountId;
    private final List<Holdings> holdings;
    private final Map<String, MarketDataDto> marketDataMap; // String (symbol-assetType) -> MarketDataDto
    private final String baseCurrency;
    private Map<String, Object[]> assetDetailsMap = new HashMap<>(); // String assetName -> Object[] (symbol, assetType)
    private Map<String, Map<String, BigDecimal>> assetValuesMap = new HashMap<>();

    public PortfolioCalculator(UUID accountId, List<Holdings> holdings, Map<String, MarketDataDto> marketDataMap, String baseCurrency) {
        this.accountId = accountId;
        this.holdings = holdings;
        this.marketDataMap = marketDataMap;
        this.baseCurrency = baseCurrency;
        this.assetDetailsMap = calculateAssetDetails();
        this.assetValuesMap = calculateAssetValues();
    }

    public UUID getAccountId() {
        return accountId;
    }

    public List<Holdings> getHoldings() {
        return holdings;
    }

    public Map<String, MarketDataDto> getMarketDataMap() {
        return marketDataMap;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public Map<String, Object[]> getAssetDetailsMap() {
        return assetDetailsMap;
    }

    public Map<String, Map<String, BigDecimal>> getAssetValuesMap() {
        return assetValuesMap;
    }

    public List<Map<String, Object>> getPortfolioData() {
        List<Map<String, Object>> portfolioData = new ArrayList<>();

        for (Holdings holding : holdings) {
            String assetName = holding.getAssetName();
            Map<String, Object> assetData = new HashMap<>();
            assetData.put("assetName", assetName);
            assetData.put("symbol", assetDetailsMap.get(assetName)[0]);
            assetData.put("assetType", assetDetailsMap.get(assetName)[1]);
            assetData.put("quantity", holding.getTotalBalance());
            assetData.put("priceInBaseCurrency", assetValuesMap.get(assetName).get("priceInBaseCurrency"));
            assetData.put("totalValueInBaseCurrency", assetValuesMap.get(assetName).get("totalValueInBaseCurrency"));

            portfolioData.add(assetData);
        }

        return portfolioData;
    }

    /**
     * Calculates a map of (asset name) -> Object[] (symbol, asset type).
     */
    public Map<String, Object[]> calculateAssetDetails() {
        Map<String, Object[]> assetDetails = new HashMap<>();

        for (Holdings holding : holdings) {
            String assetName = holding.getAssetName();
            String symbol = holding.getSymbol();
            AssetType assetType = holding.getAssetType();

            // Create Object[] pair for symbol and assetType
            Object[] symbolAssetTypePair = new Object[]{symbol, assetType};
            assetDetails.put(assetName, symbolAssetTypePair);

            logger.trace("Asset details calculated: assetName={}, symbol={}, assetType={}", assetName, symbol, assetType);
        }

        return assetDetails;
    }
    
    /**
     * Calculates a map of (asset name) -> Map<String, BigDecimal> (price in base currency, total value in base currency).
     */
    public Map<String, Map<String, BigDecimal>> calculateAssetValues() {
        Map<String, Map<String, BigDecimal>> assetValues = new HashMap<>();
    
        for (Holdings holding : holdings) {
            String assetName = holding.getAssetName();
            Object[] symbolAssetTypePair = assetDetailsMap.get(assetName);
            String symbol = (String) symbolAssetTypePair[0];
            AssetType assetType = (AssetType) symbolAssetTypePair[1];
            BigDecimal quantity = BigDecimal.valueOf(holding.getTotalBalance());
    
            // Get price in base currency
            BigDecimal priceInBaseCurrency = getPriceInBaseCurrency(symbol, assetType);
    
            // Calculate total value in base currency
            BigDecimal totalValueInBaseCurrency = priceInBaseCurrency.multiply(quantity);
    
            // Store the results
            Map<String, BigDecimal> values = new HashMap<>();
            values.put("priceInBaseCurrency", priceInBaseCurrency);
            values.put("totalValueInBaseCurrency", totalValueInBaseCurrency);
    
            assetValues.put(assetName, values);
    
            logger.trace("Asset values calculated: assetName={}, priceInBaseCurrency={}, totalValueInBaseCurrency={}",
                    assetName, priceInBaseCurrency, totalValueInBaseCurrency);
        }
    
        return assetValues;
    }

    private BigDecimal getPriceInBaseCurrency(String symbol, AssetType assetType) {
        BigDecimal priceInBaseCurrency = BigDecimal.ZERO;
    
        if (assetType == AssetType.FOREX) {
            priceInBaseCurrency = getForexPriceInBaseCurrency(symbol);
        } else {
            priceInBaseCurrency = getNonForexPriceInBaseCurrency(symbol, assetType);
        }
    
        return priceInBaseCurrency;
    }

    private BigDecimal getForexPriceInBaseCurrency(String symbol) {
        if (symbol.equals(baseCurrency)) {
            logger.trace("FOREX symbol matches base currency: symbol={}, priceInBaseCurrency=1", symbol);
            return BigDecimal.ONE;
        }
    
        // Try direct pair
        String forexKey = symbol + "/" + baseCurrency + "-" + AssetType.FOREX.getAssetTypeName();
        MarketDataDto marketData = marketDataMap.get(forexKey);
    
        if (marketData == null) {
            // Try reverse pair
            forexKey = baseCurrency + "/" + symbol + "-" + AssetType.FOREX.getAssetTypeName();
            marketData = marketDataMap.get(forexKey);
    
            if (marketData != null) {
                BigDecimal inversePrice = BigDecimal.ONE.divide(marketData.getPrice(), 4, RoundingMode.HALF_UP);
                logger.trace("Reverse FOREX pair found: forexKey={}, priceInBaseCurrency={}", forexKey, inversePrice);
                return inversePrice;
            } else {
                logger.warn("No FOREX market data found for symbol={}, baseCurrency={}", symbol, baseCurrency);
                return BigDecimal.ZERO;
            }
        }
    
        logger.trace("FOREX market data found: forexKey={}, priceInBaseCurrency={}", forexKey, marketData.getPrice());
        return marketData.getPrice();
    }

    private BigDecimal getNonForexPriceInBaseCurrency(String symbol, AssetType assetType) {
        String key = symbol + "-" + assetType.getAssetTypeName();
        MarketDataDto marketData = marketDataMap.get(key);
    
        if (marketData == null) {
            logger.warn("No market data found for symbol={}, assetType={}", symbol, assetType);
            return BigDecimal.ZERO;
        }
    
        BigDecimal priceInBaseCurrency = marketData.getPrice();
        logger.trace("Market data found: symbol={}, assetType={}, priceInBaseCurrency={}", symbol, assetType, priceInBaseCurrency);
    
        // Convert price to base currency if baseCurrency is not USD
        if (!baseCurrency.equals("USD")) {
            String forexKey = "USD/" + baseCurrency + "-" + AssetType.FOREX.getAssetTypeName();
            MarketDataDto forexMarketData = marketDataMap.get(forexKey);
    
            if (forexMarketData != null) {
                BigDecimal usdToBaseCurrencyRate = forexMarketData.getPrice();
                priceInBaseCurrency = priceInBaseCurrency.multiply(usdToBaseCurrencyRate);
                logger.trace("Converted price to base currency: forexKey={}, usdToBaseCurrencyRate={}, priceInBaseCurrency={}",
                        forexKey, usdToBaseCurrencyRate, priceInBaseCurrency);
            } else {
                logger.warn("No FOREX market data found for USD/{} conversion. Using price in USD.", baseCurrency);
            }
        }
    
        return priceInBaseCurrency;
    }
}