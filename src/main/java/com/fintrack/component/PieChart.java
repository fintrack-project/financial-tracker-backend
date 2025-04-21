package com.fintrack.component;

import java.util.*;
import java.util.stream.Collectors;

import com.fintrack.model.Holdings;
import com.fintrack.model.HoldingsCategory;
import com.fintrack.model.MarketData;
import com.fintrack.model.Category;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PieChart {

    private static final Logger logger = LoggerFactory.getLogger(PieChart.class);

    private static final List<String> COLORS = Arrays.asList(
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#FFC107", // Amber
        "#FF5722", // Deep Orange
        "#9C27B0", // Purple
        "#3F51B5", // Indigo
        "#00BCD4", // Cyan
        "#8BC34A", // Light Green
        "#FF9800", // Orange
        "#E91E63"  // Pink
    );

    private int colorIndex = 0;

    private List<Holdings> holdings;
    private List<MarketData> marketData;
    private List<HoldingsCategory> holdingsCategories;
    private List<Category> subcategories;
    private String categoryName;
    private List<PieChartData> pieChartData;
    private Map<String, String> subcategoryColorMap = new HashMap<>();
    private Double totalValue = 0.0;
    private Map<String, Double> assetValueMap = new HashMap<>();
    private Map<String, Double> subcategoryValueMap = new HashMap<>();

    public PieChart(List<Holdings> holdings, List<MarketData> marketData) {
        this.holdings = holdings;
        this.marketData = marketData;
        this.pieChartData = generatePieChartData();
    }

    public PieChart(List<Holdings> holdings, List<MarketData> marketData, List<HoldingsCategory> holdingsCategories, String categoryName) {
        this.holdings = holdings;
        this.marketData = marketData;
        this.holdingsCategories = holdingsCategories;
        this.categoryName = categoryName;
        this.pieChartData = generatePieChartDataByCategoryName(categoryName);
    }

    public PieChart(List<Holdings> holdings, List<MarketData> marketData, List<HoldingsCategory> holdingsCategories, List<Category> subcategories, String categoryName) {
        this.holdings = holdings;
        this.marketData = marketData;
        this.holdingsCategories = holdingsCategories;
        this.subcategories = subcategories;
        this.categoryName = categoryName;
        this.pieChartData = generatePieChartDataByCategoryName(categoryName);
    }

    public List<PieChartData> getPieChartData() {
        return pieChartData;
    }

    public void setPieChartData(List<PieChartData> pieChartData) {
        this.pieChartData = pieChartData;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<Map<String, Object>> getData() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (PieChartData pieChart : pieChartData) {
            Map<String, Object> map = new HashMap<>();
            map.put("assetName", pieChart.getAssetName());
            map.put("symbol", pieChart.getSymbol());
            map.put("subcategory", pieChart.getSubcategory());
            map.put("value", pieChart.getValue());
            map.put("color", pieChart.getColor());
            map.put("priority", pieChart.getPriority());
            map.put("totalValue", totalValue);
            map.put("subcategoryValue", subcategoryValueMap.getOrDefault(pieChart.getSubcategory(), 0.0));
            map.put("percentage", pieChart.getPercentage());
            map.put("percentageOfSubcategory", pieChart.getPercentageOfSubcategory());
            data.add(map);
        }
        return data;
    }

    private List<PieChartData> generatePieChartData() {
        logger.debug("Generating pie chart data for all holdings");
        Map<String, Double> symbolToPriceMap = getSymbolToPriceMap();
        List<PieChartData> pieChartData = holdings.stream()
                .map(holding -> {
                    String symbol = holding.getSymbol();
                    Double totalBalance = holding.getTotalBalance();
                    Double price = symbolToPriceMap.getOrDefault(symbol, 0.0); // Default price to 0.0 if not found
                    Double value = totalBalance * price; // Calculate value using price and total balance

                    assetValueMap.put(holding.getAssetName(), value); // Store asset value
                    totalValue += value; // Accumulate total value
                    subcategoryValueMap.put("None", subcategoryValueMap.getOrDefault("None", 0.0) + value); // Update subcategory value

                    return new PieChartData(
                      holding.getAssetName(), 
                      symbol,
                      "None",
                      value, 
                      0, // Default priority
                      getColor()); // Assign a random color
                })
                .collect(Collectors.toList());

        // Calculate percentage of each subcategory
        pieChartData.forEach(data -> {
            Double value = assetValueMap.get(data.getAssetName());
            Double percentage = (value / totalValue) * 100; // Calculate percentage
            data.setPercentage(percentage); // Set percentage in PieChartData
            data.setPercentageOfSubcategory(1.0); // Set percentage in PieChartData
        });
        pieChartData.forEach(date -> 
            logger.trace("PieChartData: " + date.getAssetName() + ", " + date.getValue() + ", " + date.getPercentage())
        );
        return pieChartData;
    }

    private List<PieChartData> generatePieChartDataByCategoryName(String categoryName) {
        logger.debug("Generating pie chart data for category: " + categoryName);
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

        List<PieChartData> pieChartData = holdings.stream()
                .filter(holding -> assetNamesSubcategoryMap.containsKey(holding.getAssetName())) // Ensure filtering by valid keys
                .map(holding -> {
                    String symbol = holding.getSymbol();
                    Double totalBalance = holding.getTotalBalance();
                    Double price = symbolToPriceMap.getOrDefault(symbol, 0.0); // Default price to 0.0 if not found
                    Double value = totalBalance * price; // Calculate value using price and total balance

                    String subcategory = assetNamesSubcategoryMap.get(holding.getAssetName());

                    // Get or generate a color for the subcategory
                    String color = subcategoryColorMap.computeIfAbsent(subcategory, key -> getColor());

                    assetValueMap.put(holding.getAssetName(), value); // Store asset value
                    totalValue += value; // Accumulate total value
                    subcategoryValueMap.put(subcategory, subcategoryValueMap.getOrDefault(subcategory, 0.0) + value); // Update subcategory value

                    return new PieChartData(
                        holding.getAssetName(),
                        symbol,
                        subcategory, 
                        value, 
                        getSubcategoryPriorityMap().getOrDefault(subcategory, 0), // Get priority from subcategory map
                        color); // Use subcategory as name
                })
                .collect(Collectors.toList());
        
        // Calculate percentage of each subcategory
        pieChartData.forEach(data -> {
            Double value = assetValueMap.get(data.getAssetName());
            Double percentage = (value / totalValue) * 100; // Calculate percentage
            data.setPercentage(percentage); // Set percentage in PieChartData
            data.setPercentageOfSubcategory(subcategoryValueMap.get(data.getSubcategory()) / totalValue * 100); // Set percentage of subcategory
        });
        pieChartData.forEach(date -> 
            logger.trace("PieChartData: " + date.getAssetName() + ", " + date.getValue() + ", " + date.getPercentage())
        );
        Collections.sort(pieChartData);
        return pieChartData;
    }

    private Map<String, Double> getSymbolToPriceMap() {
        return marketData.stream()
                .collect(Collectors.toMap(MarketData::getSymbol, marketData -> marketData.getPrice().doubleValue()));
    }

    private Map<String, Integer> getSubcategoryPriorityMap() {
        Map<String, Integer> subcategoryPriorityMap = subcategories.stream()
        .collect(Collectors.toMap(Category::getCategoryName, Category::getPriority));
        subcategoryPriorityMap.put("None", 0); // Assign priority 0 to "None" subcategory
        return subcategoryPriorityMap;
    }

    private String getColor() {
      String color = COLORS.get(colorIndex); // Get color from predefined list
      colorIndex = (colorIndex + 1) % COLORS.size(); // Cycle through colors
      return color; // Return color in hex format
  }
}
