package com.fintrack.service;

import com.fintrack.repository.*;
import com.fintrack.model.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioService {

    private static final Logger logger = LogManager.getLogger(PortfolioService.class);

    private final HoldingsRepository holdingsRepository;
    private final HoldingsCategoriesRepository holdingsCategoriesRepository;
    private final MarketDataRepository marketDataRepository;
    private final CategoriesRepository categoriesRepository;
    private final SubcategoriesRepository subcategoriesRepository;

    public PortfolioService(
            HoldingsRepository holdingsRepository,
            HoldingsCategoriesRepository holdingsCategoriesRepository,
            MarketDataRepository marketDataRepository,
            CategoriesRepository categoriesRepository,
            SubcategoriesRepository subcategoriesRepository) {
        this.holdingsRepository = holdingsRepository;
        this.holdingsCategoriesRepository = holdingsCategoriesRepository;
        this.marketDataRepository = marketDataRepository;
        this.categoriesRepository = categoriesRepository;
        this.subcategoriesRepository = subcategoriesRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> calculatePortfolioPieChartData(UUID accountId, String categoryName) {
        // Validate input
        if (accountId == null || categoryName == null || categoryName.isEmpty()) {
            throw new IllegalArgumentException("Account ID and category name must not be null or empty.");
        }

        logger.info("Calculating portfolio pie chart data for account ID: " + accountId + " and category name: " + categoryName);

        // Fetch holdings for the given account ID
        List<Holdings> holdings = holdingsRepository.findHoldingsByAccount(accountId);
        holdings.stream()
                .forEach(holding -> logger.info("Holdings: " + holding.getAssetName() + ", Symbol: " + holding.getSymbol() + ", Total Balance: " + holding.getTotalBalance()));

        // Fetch market data for the symbols
        List<String> symbols = holdings.stream()
                .map(Holdings::getSymbol)
                .distinct()
                .collect(Collectors.toList());
        List<MarketData> marketDataList = marketDataRepository.findMarketDataBySymbols(symbols);
        logger.info("Generating Piechart... Market Data: " + marketDataList);
    
        // Create a map of symbol to price for quick lookup
        Map<String, Double> symbolToPriceMap = marketDataList.stream()
                .collect(Collectors.toMap(MarketData::getSymbol, marketData -> marketData.getPrice().doubleValue()));
        logger.info("Generating Piechart... Symbol to Price Map: " + symbolToPriceMap);

        // Handle the case when categoryName is "None"
        if ("None".equalsIgnoreCase(categoryName)) {
            return generatePieChartDataForAssets(accountId, holdings, symbolToPriceMap);
        }

        // Fetch the category ID for the given account and category name
        Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("Category not found for the given account and category name.");
        }
        
        logger.info("Category ID for category name '" + categoryName + "' is: " + categoryId);

        // Fetch holdings categories for the given account ID
        List<Map<String, Object>> holdingsCategories = holdingsCategoriesRepository.findHoldingsByAccountId(accountId);

        // Filter holdings categories by the specified category
        List<Map<String, Object>> filteredHoldingsCategories = holdingsCategories.stream()
        .filter(category -> category.get("category") != null && category.get("category").equals(categoryName)) // Filter by category
        .collect(Collectors.toList());

        // Log the filtered holdings categories
        filteredHoldingsCategories.stream()
                .forEach(category -> logger.info("Holdings Categories, Asset Names:" + category.get("asset_name") + ", Category: " + category.get("category") + ", Subcategory: " + category.get("subcategory")));

        Map<String, String> assetNamesSubcategoryMap = filteredHoldingsCategories.stream()
                .collect(Collectors.toMap(
                        category -> (String) category.get("asset_name"),
                        category -> (String) category.get("subcategory"),
                        (existing, replacement) -> existing // Handle duplicate keys by keeping the existing value
                ));
        
        assetNamesSubcategoryMap.forEach((assetName, subcategoryName) -> 
                logger.info("Asset Name: {}, Subcategory Name: {}", assetName, subcategoryName));

        // Generate pie chart data based on subcategories
        Map<String, Double> subcategoryTotals = new HashMap<>();

        for (Holdings holding : holdings) {
            String symbol = holding.getSymbol();
            Double totalBalance = holding.getTotalBalance();
            String subcategory = assetNamesSubcategoryMap.getOrDefault(holding.getAssetName(), "Unnamed"); // Use "Unnamed" if subcategory is null
            logger.info("Generating Piechart... Asset Name: " + holding.getAssetName() + ", Subcategory: " + subcategory);

            // Calculate total value
            if (totalBalance != null) {
                double totalValue = totalBalance * symbolToPriceMap.get(symbol); // Placeholder for price, to be replaced with actual price from market data
                subcategoryTotals.put(subcategory, subcategoryTotals.getOrDefault(subcategory, 0.0) + totalValue);
            }
            logger.info("Generating Piechart... Subcategory: " + subcategory + ", Total Value: " + subcategoryTotals.get(subcategory));
        }

        List<Map<String, Object>> pieChartData = subcategoryTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // Sort by total value descending
                .map(entry -> Map.<String, Object>of(
                        "name", entry.getKey(),
                        "value", entry.getValue(),
                        "color", getRandomColor() // Assign a random color
                ))
                .collect(Collectors.toList());
        logger.info("Generatated pie chart data: " + pieChartData);
        return pieChartData;
    }

    private List<Map<String, Object>> generatePieChartDataForAssets(UUID accountId, List<Holdings> holdings, Map<String, Double> symbolToPriceMap) {
        // Generate pie chart data
        List<Map<String, Object>> pieChartData = holdings.stream()
                .map(holding -> {
                    String symbol = holding.getSymbol();
                    Double totalBalance = holding.getTotalBalance();
                    Double price = symbolToPriceMap.getOrDefault(symbol, 0.0); // Default price to 0.0 if not found
                    Double value = totalBalance * price; // Calculate value using price and total balance

                    logger.info("Generating Piechart... Asset Name: " + holding.getAssetName() + ", Symbol: " + symbol + ", Total Balance: " + totalBalance + ", Price: " + price + ", Value: " + value);

                    return Map.<String, Object>of(
                            "name", holding.getAssetName(),
                            "value", value,
                            "color", getRandomColor() // Assign a random color
                    );
                })
                .collect(Collectors.toList());
        logger.info("Generated pie chart data: " + pieChartData);
        return pieChartData;
    }

    private String getRandomColor() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return String.format("#%02x%02x%02x", r, g, b); // Return color in hex format
    }
}