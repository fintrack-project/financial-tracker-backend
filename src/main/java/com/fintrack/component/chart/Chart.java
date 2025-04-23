package com.fintrack.component.chart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fintrack.constants.Color;
import com.fintrack.model.Category;
import com.fintrack.model.Holdings;
import com.fintrack.model.HoldingsCategory;
import com.fintrack.model.MarketData;
import com.fintrack.model.MarketDataDto;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Chart {

    private static final Logger logger = LoggerFactory.getLogger(PieChart.class);

    private static final List<Color> COLORS = Color.getAllColors(); // Use the Color enum to get all colors

    private int colorIndex = 0;

    private List<Holdings> holdings;
    private List<MarketDataDto> marketDataDto;
    private List<HoldingsCategory> holdingsCategories;
    private List<Category> subcategories;
    private List<ChartData> chartData;
    private String categoryName;
    private Map<String, String> subcategoryColorMap = new HashMap<>();
    private Double totalValue = 0.0;
    private Map<String, Double> assetValueMap = new HashMap<>();
    private Map<String, Double> subcategoryValueMap = new HashMap<>();

    public Chart(List<Holdings> holdings, List<MarketDataDto> marketDataDto) {
        this.holdings = holdings;
        this.marketDataDto = marketDataDto;
        this.chartData = generateChartData();
    }

    public Chart(List<Holdings> holdings, List<MarketDataDto> marketDataDto, List<HoldingsCategory> holdingsCategories, String categoryName) {
        this.holdings = holdings;
        this.marketDataDto = marketDataDto;
        this.holdingsCategories = holdingsCategories;
        this.categoryName = categoryName;
        this.chartData = generateChartDataByCategoryName(categoryName);
    }

    public Chart(List<Holdings> holdings, List<MarketDataDto> marketDataDto, List<HoldingsCategory> holdingsCategories, List<Category> subcategories, String categoryName) {
        this.holdings = holdings;
        this.marketDataDto = marketDataDto;
        this.holdingsCategories = holdingsCategories;
        this.subcategories = subcategories;
        this.categoryName = categoryName;
        this.chartData = generateChartDataByCategoryName(categoryName);
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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
        logger.trace("Generating chart data for all holdings");
        Map<String, Double> symbolToPriceMap = getSymbolToPriceMap();
        List<ChartData> chartData = holdings.stream()
                .map(holding -> {
                    String symbol = holding.getSymbol();
                    Double totalBalance = holding.getTotalBalance();
                    Double price = symbolToPriceMap.getOrDefault(symbol, 0.0); // Default price to 0.0 if not found
                    Double value = totalBalance * price; // Calculate value using price and total balance

                    assetValueMap.put(holding.getAssetName(), value); // Store asset value
                    totalValue += value; // Accumulate total value
                    subcategoryValueMap.put("None", subcategoryValueMap.getOrDefault("None", 0.0) + value); // Update subcategory value

                    return new ChartData(
                        holding.getAssetName(), 
                        symbol,
                        "None",
                        value, 
                        0, // Default priority
                        assignColor()); // Assign a random color
                })
                .sorted(Comparator.comparing(ChartData::getValue).reversed()) // Sort by asset value in descending order
                .collect(Collectors.toList()
                );
        return chartData;
    }

    private List<ChartData> generateChartDataByCategoryName(String categoryName) {
        logger.trace("Generating chart data for category: " + categoryName);
        // Generate pie chart data for a specific category
        Map<String, Double> symbolToPriceMap = getSymbolToPriceMap();

        List<HoldingsCategory> filteredHoldingsCategories = holdingsCategories.stream()
        .filter(category -> category.getCategory() != null && category.getCategory().equals(categoryName)) // Filter by category
        .collect(Collectors.toList());

        filteredHoldingsCategories.forEach(category -> 
            logger.trace("Filtered HoldingsCategory: " + category.getAssetName() + ", " + category.getSubcategory())
        );

        Map<String, String> assetNamesSubcategoryMap = filteredHoldingsCategories.stream()
            .collect(Collectors.toMap(
                HoldingsCategory::getAssetName,
                category -> category.getSubcategory() != null ? category.getSubcategory() : "None", // Assign "None" if subcategory is null
                (existing, replacement) -> existing // Handle duplicate keys by keeping the existing value
            ));
        
        assetNamesSubcategoryMap.forEach((key, value) ->
            logger.trace("Asset Name: " + key + ", Subcategory: " + value)
        );

        List<ChartData> chartData = holdings.stream()
                .filter(holding -> assetNamesSubcategoryMap.containsKey(holding.getAssetName())) // Ensure filtering by valid keys
                .map(holding -> {
                    String symbol = holding.getSymbol();
                    Double totalBalance = holding.getTotalBalance();
                    Double price = symbolToPriceMap.getOrDefault(symbol, 0.0); // Default price to 0.0 if not found
                    Double value = totalBalance * price; // Calculate value using price and total balance

                    String subcategory = assetNamesSubcategoryMap.get(holding.getAssetName());

                    // Get or generate a color for the subcategory
                    String color = subcategoryColorMap.computeIfAbsent(subcategory, key -> assignColor());

                    assetValueMap.put(holding.getAssetName(), value); // Store asset value
                    totalValue += value; // Accumulate total value
                    subcategoryValueMap.put(subcategory, subcategoryValueMap.getOrDefault(subcategory, 0.0) + value); // Update subcategory value

                    return new ChartData(
                        holding.getAssetName(),
                        symbol,
                        subcategory, 
                        value, 
                        getSubcategoryPriorityMap().getOrDefault(subcategory, 0), // Get priority from subcategory map
                        color); // Use subcategory as name
                })
                .sorted(Comparator
                    .comparing(ChartData::getPriority) // Sort by priority
                    .thenComparing(ChartData::getValue).reversed()) // Sort by asset value
                .collect(Collectors.toList());
        
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
