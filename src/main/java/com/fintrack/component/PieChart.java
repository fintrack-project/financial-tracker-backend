package com.fintrack.component;

import java.util.*;
import java.util.stream.Collectors;

import com.fintrack.model.Holdings;
import com.fintrack.model.HoldingsCategory;
import com.fintrack.model.MarketData;

public class PieChart {

    private List<Holdings> holdings;
    private List<MarketData> marketData;
    private List<HoldingsCategory> holdingsCategories;
    private String categoryName;
    private List<PieChartData> pieChartData;

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
            map.put("name", pieChart.getName());
            map.put("value", pieChart.getValue());
            map.put("color", pieChart.getColor());
            data.add(map);
        }
        return data;
    }

    private List<PieChartData> generatePieChartData() {
        Map<String, Double> symbolToPriceMap = getSymbolToPriceMap();
        List<PieChartData> pieChartData = holdings.stream()
                .map(holding -> {
                    String symbol = holding.getSymbol();
                    Double totalBalance = holding.getTotalBalance();
                    Double price = symbolToPriceMap.getOrDefault(symbol, 0.0); // Default price to 0.0 if not found
                    Double value = totalBalance * price; // Calculate value using price and total balance

                    return new PieChartData(holding.getAssetName(), value, getRandomColor()); // Assign a random color
                })
                .collect(Collectors.toList());
        return pieChartData;
    }

    private List<PieChartData> generatePieChartDataByCategoryName(String categoryName) {
        // Generate pie chart data for a specific category
        Map<String, Double> symbolToPriceMap = getSymbolToPriceMap();

        List<HoldingsCategory> filteredHoldingsCategories = holdingsCategories.stream()
        .filter(category -> category.getCategory() != null && category.getCategory().equals(categoryName)) // Filter by category
        .collect(Collectors.toList());

        Map<String, String> assetNamesSubcategoryMap = filteredHoldingsCategories.stream()
            .collect(Collectors.toMap(
                HoldingsCategory::getAssetName,
                category -> category.getSubcategory() != null ? category.getSubcategory() : "Unnamed", // Assign "Unnamed" if subcategory is null
                (existing, replacement) -> existing // Handle duplicate keys by keeping the existing value
            ));
        
        List<PieChartData> pieChartData = holdings.stream()
                .filter(holding -> assetNamesSubcategoryMap.containsKey(holding.getAssetName())) // Ensure filtering by valid keys
                .map(holding -> {
                    String symbol = holding.getSymbol();
                    Double totalBalance = holding.getTotalBalance();
                    Double price = symbolToPriceMap.getOrDefault(symbol, 0.0); // Default price to 0.0 if not found
                    Double value = totalBalance * price; // Calculate value using price and total balance

                    return new PieChartData(assetNamesSubcategoryMap.get(holding.getAssetName()), value, getRandomColor()); // Use subcategory as name
                })
                .collect(Collectors.toList());
        return pieChartData;
    }

    private Map<String, Double> getSymbolToPriceMap() {
        return marketData.stream()
                .collect(Collectors.toMap(MarketData::getSymbol, marketData -> marketData.getPrice().doubleValue()));
    }

    private String getRandomColor() {
      Random random = new Random();
      int r = random.nextInt(256);
      int g = random.nextInt(256);
      int b = random.nextInt(256);
      return String.format("#%02x%02x%02x", r, g, b); // Return color in hex format
  }
}
