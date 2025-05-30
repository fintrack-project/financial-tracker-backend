package com.fintrack.component.chart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fintrack.component.calculator.PortfolioCalculator;
import com.fintrack.model.finance.Category;
import com.fintrack.model.finance.HoldingsCategory;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class BarChart extends Chart {
    private static final Logger logger = LoggerFactory.getLogger(BarChart.class);
    
    private List<BarChartData> barChartDatas;
    private LocalDate localDate;

    public BarChart(PortfolioCalculator portfolioCalculator) {
        super(portfolioCalculator);
        this.barChartDatas = generateBarChartData();
    }

    public BarChart(PortfolioCalculator portfolioCalculator, List<HoldingsCategory> holdingsCategories, Category category) {
        super(portfolioCalculator, holdingsCategories, category);
        this.barChartDatas = generateBarChartDataByCategory(category);
    }

    public BarChart(PortfolioCalculator portfolioCalculator, List<HoldingsCategory> holdingsCategories, List<Category> subcategories, Category category) {
        super(portfolioCalculator, holdingsCategories, subcategories, category);
        this.barChartDatas = generateBarChartDataByCategory(category);
    }

    public List<BarChartData> getBarChartDatas() {
        return barChartDatas;
    }

    public void setBarChartDatas(List<BarChartData> barChartDatas) {
        this.barChartDatas = barChartDatas;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public Map<String, Object> getDataByDate() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (BarChartData barChartData : barChartDatas) {
            Map<String, Object> map = new HashMap<>();
            map.put("assetName", barChartData.getAssetName());
            map.put("symbol", barChartData.getSymbol());
            map.put("subcategory", barChartData.getSubcategory());
            map.put("value", barChartData.getValue());
            map.put("color", barChartData.getColor());
            map.put("priority", barChartData.getPriority());
            map.put("totalValue", getTotalValue());
            map.put("subcategoryValue", getSubcategoryValueMap().getOrDefault(barChartData.getSubcategory(), 0.0));
            map.put("percentage", barChartData.getPercentage());
            map.put("percentageOfSubcategory", barChartData.getPercentageOfSubcategory());
            data.add(map);
        }
        Map<String, Object> totalMap = new HashMap<>();
        totalMap.put(getLocalDate().toString(), data);
        return totalMap;
    }

    private List<BarChartData> generateBarChartData() {
        List<BarChartData> barChartDatas = getChartData().stream().map(
            data -> {
                BarChartData barChartDatum = new BarChartData(data);
                Double value = getAssetValueMap().get(data.getAssetName());
                Double percentage = (value / getTotalValue()) * 100; // Calculate percentage
                barChartDatum.setPercentage(percentage); // Set percentage in PieChartData
                barChartDatum.setPercentageOfSubcategory(100.0); // Set percentage of subcategory to 1.0
                return barChartDatum;
            }
        ).collect(Collectors.toList());
        return barChartDatas;
    }

    private List<BarChartData> generateBarChartDataByCategory(Category category) {
        List<BarChartData> barChartDatas = getChartData().stream().map(
            data -> {
                BarChartData barChartDatum = new BarChartData(data);
                Double value = getAssetValueMap().get(data.getAssetName());
                Double percentage = (value / getTotalValue()) * 100; // Calculate percentage
                barChartDatum.setPercentage(percentage); // Set percentage in PieChartData
                barChartDatum.setPercentageOfSubcategory((getSubcategoryValueMap().getOrDefault(barChartDatum.getSubcategory(), 0.0)/getTotalValue()) * 100); // Set percentage of subcategory
                return barChartDatum;
            }
        ).collect(Collectors.toList());
        return barChartDatas;
    }
}
