package com.fintrack.component.calculator;

import com.fintrack.constants.AssetType;
import com.fintrack.model.Holdings;
import com.fintrack.model.MarketDataDto;
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
     * Calculates a map of (asset name) -> (price in base currency, total value in base currency).
     */
    public Map<String, Map<String, BigDecimal>> calculateAssetValues() {
        Map<String, Map<String, BigDecimal>> assetValues = new HashMap<>();

        for (Holdings holding : holdings) {
            String assetName = holding.getAssetName();
            Object[] symbolAssetTypePair = assetDetailsMap.get(assetName);
            String symbol = (String) symbolAssetTypePair[0];
            AssetType assetType = (AssetType) symbolAssetTypePair[1];
            BigDecimal quantity = BigDecimal.valueOf(holding.getTotalBalance());

            BigDecimal priceInBaseCurrency = BigDecimal.ZERO;

            // Handle FOREX symbols
            if (assetType == AssetType.FOREX) {
                if (symbol.equals(baseCurrency)) {
                    // If the FOREX symbol is the same as the base currency, set price to 1
                    priceInBaseCurrency = BigDecimal.ONE;
                    logger.trace("FOREX symbol matches base currency: symbol={}, priceInBaseCurrency={}", symbol, priceInBaseCurrency);
                } else {
                    // Fetch the correct FOREX market data
                    String forexKey = symbol + "/" + baseCurrency + "-" + AssetType.FOREX.getAssetTypeName();
                    MarketDataDto marketData = marketDataMap.get(forexKey);

                    logger.trace("FOREX market data lookup: forexKey={}, marketData={}", forexKey, marketData);

                    if (marketData == null) {
                        // Try the reverse pair (e.g., USD/AUD instead of AUD/USD)
                        forexKey = baseCurrency + "/" + symbol + "-" + AssetType.FOREX.getAssetTypeName();
                        marketData = marketDataMap.get(forexKey);

                        if (marketData != null) {
                            // If reverse pair exists, calculate the inverse price
                            priceInBaseCurrency = BigDecimal.ONE.divide(marketData.getPrice(), 4, RoundingMode.HALF_UP);
                            logger.trace("Reverse FOREX pair found: forexKey={}, priceInBaseCurrency={}", forexKey, priceInBaseCurrency);
                        }
                    } else {
                        priceInBaseCurrency = marketData.getPrice();
                        logger.trace("FOREX market data found: forexKey={}, priceInBaseCurrency={}", forexKey, priceInBaseCurrency);
                    }
                }
            } else {
                // Handle non-FOREX symbols
                MarketDataDto marketData = marketDataMap.get(symbol+ "-" + assetType.getAssetTypeName());
                if (marketData != null) {
                    priceInBaseCurrency = marketData.getPrice();
                    logger.trace("Market data found: symbol={}, assetType={}, priceInBaseCurrency={}", symbol, assetType, priceInBaseCurrency);
                }
            }

            logger.trace("Calculating asset values: assetName={}, symbol={}, assetType={}, quantity={}, priceInBaseCurrency={}",
                    assetName, symbol, assetType, quantity, priceInBaseCurrency);

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
}