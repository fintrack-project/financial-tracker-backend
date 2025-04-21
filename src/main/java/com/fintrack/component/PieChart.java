package com.fintrack.component;

import java.util.*;

import com.fintrack.model.Holdings;
import com.fintrack.model.HoldingsCategory;
import com.fintrack.model.MarketData;
import com.fintrack.model.Category;

import java.util.stream.Collectors;

public class PieChart extends Chart {

    private List<PieChartData> pieChartData;

    public PieChart(List<Holdings> holdings, List<MarketData> marketData) {
        super(holdings, marketData);
        this.pieChartData = generatePieChartData();
    }

    public PieChart(List<Holdings> holdings, List<MarketData> marketData, List<HoldingsCategory> holdingsCategories, String categoryName) {
        super(holdings, marketData, holdingsCategories, categoryName);
        this.pieChartData = generatePieChartDataByCategoryName(categoryName);
    }

    public PieChart(List<Holdings> holdings, List<MarketData> marketData, List<HoldingsCategory> holdingsCategories, List<Category> subcategories, String categoryName) {
        super(holdings, marketData, holdingsCategories, subcategories, categoryName);
        this.pieChartData = generatePieChartDataByCategoryName(categoryName);
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
                pieChartDatum.setPercentageOfSubcategory(1.0); // Set percentage of subcategory to 1.0
                return pieChartDatum;
            }
        ).collect(Collectors.toList());
        return pieChartDatas;
    }

    private List<PieChartData> generatePieChartDataByCategoryName(String categoryName) {
        List<PieChartData> pieChartDatas = getChartData().stream().map(
            data -> {
                PieChartData pieChartDatum = new PieChartData(data);
                Double value = getAssetValueMap().get(data.getAssetName());
                Double percentage = (value / getTotalValue()) * 100; // Calculate percentage
                pieChartDatum.setPercentage(percentage); // Set percentage in PieChartData
                pieChartDatum.setPercentageOfSubcategory(1.0); // Set percentage of subcategory to 1.0
                return pieChartDatum;
            }
        ).collect(Collectors.toList());
        return pieChartDatas;
    }
}
