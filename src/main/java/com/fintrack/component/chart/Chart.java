package com.fintrack.component.chart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fintrack.component.calculator.PortfolioCalculator;
import com.fintrack.constants.Color;
import com.fintrack.dto.market.MarketDataDto;
import com.fintrack.model.finance.Category;
import com.fintrack.model.finance.Holdings;
import com.fintrack.model.finance.HoldingsCategory;

import java.util.*;
import java.util.stream.Collectors;
import java.math.BigDecimal;

public abstract class Chart {

    private static final Logger logger = LoggerFactory.getLogger(PieChart.class);

    private static final List<Color> COLORS = Color.getAllColors(); // Use the Color enum to get all colors

    private int colorIndex = 0;

    private List<Holdings> holdings;
    private List<MarketDataDto> marketDataDto;
    private List<HoldingsCategory> holdingsCategories;
    private List<Category> subcategories;
    private List<ChartData> chartData;
    private Category category;
    private Map<String, String> subcategoryColorMap = new HashMap<>();
    private Double totalValue = 0.0;
    private Map<String, Double> assetValueMap = new HashMap<>();
    private Map<String, Double> subcategoryValueMap = new HashMap<>();

    private PortfolioCalculator portfolioCalculator;

    public Chart(PortfolioCalculator portfolioCalculator) {
        this.portfolioCalculator = portfolioCalculator;
        this.holdings = portfolioCalculator.getHoldings();
        this.chartData = generateChartData();
    }

    public Chart(PortfolioCalculator portfolioCalculator, List<HoldingsCategory> holdingsCategories, Category category) {
        this.portfolioCalculator = portfolioCalculator;
        this.holdings = portfolioCalculator.getHoldings();
        this.holdingsCategories = holdingsCategories;
        this.category = category;
        this.chartData = generateChartDataByCategory(category);
    }

    public Chart(PortfolioCalculator portfolioCalculator, List<HoldingsCategory> holdingsCategories, List<Category> subcategories, Category category) {
        this.portfolioCalculator = portfolioCalculator;
        this.holdings = portfolioCalculator.getHoldings();
        this.holdingsCategories = holdingsCategories;
        this.subcategories = subcategories;
        this.category = category;
        this.chartData = generateChartDataByCategory(category);
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<Holdings> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<Holdings> holdings) {
        this.holdings = holdings;
    }

    public List<MarketDataDto> getMarketDataDto() {
        return marketDataDto;
    }

    public void setMarketDataDto(List<MarketDataDto> marketDataDto) {
        this.marketDataDto = marketDataDto;
    }

    public List<HoldingsCategory> getHoldingsCategories() {
        return holdingsCategories;
    }

    public void setHoldingsCategories(List<HoldingsCategory> holdingsCategories) {
        this.holdingsCategories = holdingsCategories;
    }

    public List<Category> getSubcategories() {
        return subcategories;
    }

    public void setSubcategories(List<Category> subcategories) {
        this.subcategories = subcategories;
    }

    public List<ChartData> getChartData() {
        return chartData;
    }

    public void setChartData(List<ChartData> chartData) {
        this.chartData = chartData;
    }

    public Map<String, Double> getAssetValueMap() {
        return assetValueMap;
    }

    public Map<String, Double> getSubcategoryValueMap() {
        return subcategoryValueMap;
    }

    public Double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(Double totalValue) {
        this.totalValue = totalValue;
    }

    protected List<ChartData> generateChartData() {
        logger.trace("Generating chart data using PortfolioCalculator");
    
        // Get pre-calculated asset values from PortfolioCalculator
        Map<String, Map<String, BigDecimal>> assetValues = portfolioCalculator.calculateAssetValues();
        Map<String, Object[]> assetDetailsMap = portfolioCalculator.getAssetDetailsMap();
    
        // Iterate over asset values to generate chart data
        List<ChartData> chartData = assetValues.entrySet().stream()
                .map(entry -> {
                    String assetName = entry.getKey();
                    String symbol = (String) assetDetailsMap.get(assetName)[0];
                    Map<String, BigDecimal> values = entry.getValue();
                    BigDecimal priceInBaseCurrency = values.get("priceInBaseCurrency");
                    BigDecimal totalValueInBaseCurrency = values.get("totalValueInBaseCurrency");

                    logger.trace("Asset Name: {}, Symbol: {}, Price in Base Currency: {}, Total Value in Base Currency: {}",
                            assetName, symbol, priceInBaseCurrency, totalValueInBaseCurrency);
    
                    // Assign a color for the asset
                    String color = assignColor();
    
                    // Update total value
                    totalValue += totalValueInBaseCurrency.doubleValue();
    
                    // Store asset value
                    assetValueMap.put(assetName, totalValueInBaseCurrency.doubleValue());
    
                    // Create ChartData object
                    return new ChartData(
                            assetName,
                            symbol,
                            "None", // Default subcategory
                            totalValueInBaseCurrency.doubleValue(),
                            0, // Default priority
                            color
                    );
                })
                .sorted(Comparator.comparing(ChartData::getValue).reversed()) // Sort by asset value in descending order
                .collect(Collectors.toList());
        
        logger.trace("Generated chart data: {}", chartData);
        return chartData;
    }

    private List<ChartData> generateChartDataByCategory(Category category) {
        logger.trace("Generating chart data for category: {}", category.getCategoryName());
    
        // Get pre-calculated asset values from PortfolioCalculator
        Map<String, Map<String, BigDecimal>> assetValues = portfolioCalculator.calculateAssetValues();
        Map<String, Object[]> assetDetailsMap = portfolioCalculator.getAssetDetailsMap();
    
        // Filter holdings by category
        Map<String, String> assetNamesSubcategoryMap = holdingsCategories.stream()
                .filter(holdingsCategory -> holdingsCategory.getCategory() != null && holdingsCategory.getCategory().equals(category.getCategoryName()))
                .collect(Collectors.toMap(
                        HoldingsCategory::getAssetName,
                        holdingsCategory -> holdingsCategory.getSubcategory() != null ? holdingsCategory.getSubcategory() : "None",
                        (existing, replacement) -> existing
                ));
        
        logger.trace("Filtered asset names and subcategories: {}", assetNamesSubcategoryMap);

        // Create a map of subcategory names to their colors from the subcategories list
        final Map<String, String> subcategoryColorMap = new HashMap<>();
        if (subcategories != null) {
            subcategories.stream()
                .forEach(subcategory -> 
                    subcategoryColorMap.put(
                        subcategory.getCategoryName(),
                        subcategory.getColor() != null ? subcategory.getColor() : "#0000FF" // Default to blue if no color set
                    )
                );
        }
        // Add "None" subcategory with a default color
        subcategoryColorMap.put("None", "#0000FF"); // Blue color for "None" subcategory
    
        // Generate chart data
        List<ChartData> chartData = assetValues.entrySet().stream()
                .filter(entry -> assetNamesSubcategoryMap.containsKey(entry.getKey())) // Filter by valid keys
                .map(entry -> {
                    String assetName = entry.getKey();
                    String symbol = (String) assetDetailsMap.get(assetName)[0];
                    Map<String, BigDecimal> values = entry.getValue();
                    BigDecimal totalValueInBaseCurrency = values.get("totalValueInBaseCurrency");
    
                    String subcategory = assetNamesSubcategoryMap.get(assetName);
                    String color = subcategoryColorMap.getOrDefault(subcategory, "#0000FF"); // Default to blue if no color found

                    logger.trace("Asset Name: {}, Symbol: {}, Subcategory: {}, Total Value in Base Currency: {}, Color: {}",
                            assetName, symbol, subcategory, totalValueInBaseCurrency, color);
    
                    // Update total value and subcategory value
                    totalValue += totalValueInBaseCurrency.doubleValue();

                    assetValueMap.put(assetName, totalValueInBaseCurrency.doubleValue());

                    subcategoryValueMap.put(subcategory, subcategoryValueMap.getOrDefault(subcategory, 0.0) + totalValueInBaseCurrency.doubleValue());
    
                    return new ChartData(
                            assetName,
                            symbol,
                            subcategory,
                            totalValueInBaseCurrency.doubleValue(),
                            getSubcategoryPriorityMap().getOrDefault(subcategory, 0),
                            color
                    );
                })
                .sorted(Comparator
                        .comparing(ChartData::getPriority)
                        .thenComparing(ChartData::getValue).reversed())
                .collect(Collectors.toList());

        logger.trace("Generated chart data for category {}: {}", category.getCategoryName(), chartData);
        return chartData;
    }

    private Map<String, Double> getSymbolToPriceMap() {
        return marketDataDto.stream()
                .collect(Collectors.toMap(MarketDataDto::getSymbol, marketDataDto -> marketDataDto.getPrice().doubleValue()));
    }

    private Map<String, Integer> getSubcategoryPriorityMap() {
        Map<String, Integer> subcategoryPriorityMap = subcategories.stream()
        .collect(Collectors.toMap(Category::getCategoryName, Category::getPriority));
        subcategoryPriorityMap.put("None", 0); // Assign priority 0 to "None" subcategory
        return subcategoryPriorityMap;
    }

    private String assignColor() {
      String color = COLORS.get(colorIndex).getHexCode(); // Get color from predefined list
      colorIndex = (colorIndex + 1) % COLORS.size(); // Cycle through colors
      return color; // Return color in hex format
    }
}
