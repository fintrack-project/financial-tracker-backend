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

        // Handle the case when categoryName is "None"
        if ("None".equalsIgnoreCase(categoryName)) {
            return generatePieChartDataForAssets(accountId);
        }

        // Fetch the category ID for the given account and category name
        Integer categoryId = categoriesRepository.findCategoryIdByAccountIdAndCategoryName(accountId, categoryName);
        if (categoryId == null) {
            throw new IllegalArgumentException("Category not found for the given account and category name.");
        }
        
        logger.info("Category ID for category name '" + categoryName + "' is: " + categoryId);

        // Fetch subcategories for the given category ID
        List<Category> subcategories = subcategoriesRepository.findSubcategoriesByParentId(accountId, categoryId);
        
        subcategories.forEach(subcategory -> {
            logger.info("Subcategory: " + subcategory.getCategoryName());
        });

        // Fetch asset name and subcategory map for the category
        List<Map<String, Object>> assetNamesSubcategoryEntries = holdingsCategoriesRepository.findHoldingsByCategoryId(accountId, categoryId);
        Map<String, Object> assetNamesSubcategoryMap = assetNamesSubcategoryEntries.stream()
            .collect(Collectors.toMap(
                entry -> (String) entry.get("asset_name"),
                entry -> entry.get("subcategory_name")
            ));

        // Print the asset names and subcategories
        assetNamesSubcategoryMap.forEach((assetName, subcategory) -> {
            logger.info("Asset Name: " + assetName + ", Subcategory: " + subcategory);
        });

        // Fetch holdings for the given account ID
        List<Holdings> holdings = holdingsRepository.findHoldingsByAccount(accountId);

        // Generate pie chart data based on subcategories
        return generatePieChartDataForSubcategories(holdings, assetNamesSubcategoryMap);
    }


    private List<Map<String, Object>> generatePieChartDataForAssets(UUID accountId) {
        // Fetch holdings for the given account ID
        List<Holdings> holdings = holdingsRepository.findHoldingsByAccount(accountId);
    
        // Fetch market data for the symbols
        List<String> symbols = holdings.stream()
                .map(Holdings::getSymbol)
                .distinct()
                .collect(Collectors.toList());
        List<MarketData> marketDataList = marketDataRepository.findMarketDataBySymbols(symbols);
    
        // Create a map of symbol to price for quick lookup
        Map<String, Double> symbolToPriceMap = marketDataList.stream()
                .collect(Collectors.toMap(MarketData::getSymbol, marketData -> marketData.getPrice().doubleValue()));
    
        // Generate pie chart data
        return holdings.stream()
                .map(holding -> {
                    String symbol = holding.getSymbol();
                    Double totalBalance = holding.getTotalBalance();
                    Double price = symbolToPriceMap.getOrDefault(symbol, 0.0); // Default price to 0.0 if not found
                    Double value = totalBalance * price; // Calculate value using price and total balance
    
                    return Map.<String, Object>of(
                            "name", holding.getAssetName(),
                            "value", value,
                            "color", getRandomColor() // Assign a random color
                    );
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> generatePieChartDataForSubcategories(List<Holdings> holdings, Map<String, Object> assetNamesSubcategoryMap) {
        Map<String, Double> subcategoryTotals = new HashMap<>();

        for (Holdings holding : holdings) {
            String symbol = holding.getSymbol();
            Double totalBalance = holding.getTotalBalance();
            String subcategory = (String) assetNamesSubcategoryMap.get(holding.getAssetName());

            // Use "Unnamed" if subcategory is null
            if (subcategory == null) {
                subcategory = "Unnamed";
            }

            // Calculate total value
            if (totalBalance != null) {
                double totalValue = totalBalance * 0; // Placeholder for price, to be replaced with actual price from market data
                subcategoryTotals.put(subcategory, subcategoryTotals.getOrDefault(subcategory, 0.0) + totalValue);
            }
        }

        return subcategoryTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())) // Sort by total value descending
                .map(entry -> Map.<String, Object>of(
                        "name", entry.getKey(),
                        "value", entry.getValue(),
                        "color", getRandomColor() // Assign a random color
                ))
                .collect(Collectors.toList());
    }

    private String getRandomColor() {
        Random random = new Random();
        int r = random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        return String.format("#%02x%02x%02x", r, g, b); // Return color in hex format
    }
}