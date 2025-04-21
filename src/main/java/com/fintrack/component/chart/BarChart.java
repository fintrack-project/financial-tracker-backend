package com.fintrack.component.chart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fintrack.model.HoldingsCategory;
import com.fintrack.model.MarketData;
import com.fintrack.model.Category;
import com.fintrack.model.Holdings;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class BarChart extends Chart {
    private static final Logger logger = LoggerFactory.getLogger(BarChart.class);
    
    private List<BarChartData> barChartData;
            LocalDate localDate;

    public BarChart(List<Holdings> holdings, List<MarketData> marketData) {
        super(holdings, marketData);
        this.barChartData = generateBarChartData();
    }

    public BarChart(List<Holdings> holdings, List<MarketData> marketData, List<HoldingsCategory> holdingsCategories, String categoryName) {
        super(holdings, marketData, holdingsCategories, categoryName);
        this.barChartData = generateBarChartDataByCategoryName(categoryName);
    }

    public BarChart(List<Holdings> holdings, List<MarketData> marketData, List<HoldingsCategory> holdingsCategories, List<Category> subcategories, String categoryName) {
        super(holdings, marketData, holdingsCategories, subcategories, categoryName);
        this.barChartData = generateBarChartDataByCategoryName(categoryName);
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public Map<String, Object> getDataByDate() {
        List<Map<String, Object>> data = new ArrayList<>();
        for (BarChartData barChart : barChartData) {
            Map<String, Object> map = new HashMap<>();
            map.put("assetName", barChart.getAssetName());
            map.put("symbol", barChart.getSymbol());
            map.put("subcategory", barChart.getSubcategory());
            map.put("value", barChart.getValue());
            map.put("color", barChart.getColor());
            map.put("priority", barChart.getPriority());
            map.put("totalValue", getTotalValue());
            map.put("subcategoryValue", getSubcategoryValueMap().getOrDefault(barChart.getSubcategory(), 0.0));
            map.put("percentage", barChart.getPercentage());
            map.put("percentageOfSubcategory", barChart.getPercentageOfSubcategory());
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
                barChartDatum.setPercentageOfSubcategory(1.0); // Set percentage of subcategory to 1.0
                return barChartDatum;
            }
        ).collect(Collectors.toList());
        return barChartDatas;
    }

    private List<BarChartData> generateBarChartDataByCategoryName(String categoryName) {
        List<BarChartData> barChartDatas = getChartData().stream().map(
            data -> {
              BarChartData barChartDatum = new BarChartData(data);
                Double value = getAssetValueMap().get(data.getAssetName());
                Double percentage = (value / getTotalValue()) * 100; // Calculate percentage
                barChartDatum.setPercentage(percentage); // Set percentage in PieChartData
                barChartDatum.setPercentageOfSubcategory(1.0); // Set percentage of subcategory to 1.0
                return barChartDatum;
            }
        ).collect(Collectors.toList());
        return barChartDatas;
    }
}
