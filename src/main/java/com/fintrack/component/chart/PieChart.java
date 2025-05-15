package com.fintrack.component.chart;

import java.util.*;

import com.fintrack.model.finance.Category;
import com.fintrack.model.finance.HoldingsCategory;
import com.fintrack.component.calculator.PortfolioCalculator;

import java.util.stream.Collectors;

public class PieChart extends Chart {

    private List<PieChartData> pieChartData;

    public PieChart(PortfolioCalculator portfolioCalculator) {
        super(portfolioCalculator);
        this.pieChartData = generatePieChartData();
    }

    public PieChart(PortfolioCalculator portfolioCalculator, List<HoldingsCategory> holdingsCategories, Category category) {
        super(portfolioCalculator, holdingsCategories, category);
        this.pieChartData = generatePieChartDataByCategory(category);
    }

    public PieChart(PortfolioCalculator portfolioCalculator, List<HoldingsCategory> holdingsCategories, List<Category> subcategories, Category category) {
        super(portfolioCalculator, holdingsCategories, subcategories, category);
        this.pieChartData = generatePieChartDataByCategory(category);
    }

    public List<PieChartData> getPieChartData() {
        return pieChartData;
    }

    public void setPieChartData(List<PieChartData> pieChartData) {
        this.pieChartData = pieChartData;
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
            map.put("totalValue", getTotalValue());
            map.put("subcategoryValue", getSubcategoryValueMap().getOrDefault(pieChart.getSubcategory(), 0.0));
            map.put("percentage", pieChart.getPercentage());
            map.put("percentageOfSubcategory", pieChart.getPercentageOfSubcategory());
            data.add(map);
        }
        return data;
    }

    private List<PieChartData> generatePieChartData() {
        List<PieChartData> pieChartDatas = getChartData().stream().map(
            data -> {
                PieChartData pieChartDatum = new PieChartData(data);
                Double value = getAssetValueMap().get(data.getAssetName());
                Double percentage = (value / getTotalValue()) * 100; // Calculate percentage
                pieChartDatum.setPercentage(percentage); // Set percentage in PieChartData
                pieChartDatum.setPercentageOfSubcategory(100.0); // Set percentage of subcategory to 1.0
                return pieChartDatum;
            }
        ).collect(Collectors.toList());
        return pieChartDatas;
    }

    private List<PieChartData> generatePieChartDataByCategory(Category category) {
        List<PieChartData> pieChartDatas = getChartData().stream().map(
            data -> {
                PieChartData pieChartDatum = new PieChartData(data);
                Double value = getAssetValueMap().get(data.getAssetName());
                Double percentage = (value / getTotalValue()) * 100; // Calculate percentage
                pieChartDatum.setPercentage(percentage); // Set percentage in PieChartData
                pieChartDatum.setPercentageOfSubcategory((getSubcategoryValueMap().getOrDefault(pieChartDatum.getSubcategory(), 0.0)/getTotalValue()) * 100); // Set percentage of subcategory
                return pieChartDatum;
            }
        ).collect(Collectors.toList());
        return pieChartDatas;
    }
}
