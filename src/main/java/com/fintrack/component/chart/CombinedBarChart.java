package com.fintrack.component.chart;

import java.time.LocalDate;
import java.util.*;

import com.fintrack.model.finance.Category;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombinedBarChart {

    private static final Logger logger = LoggerFactory.getLogger(CombinedBarChart.class);

    private List<BarChart> barCharts;
    List<Map<String, Object>> combinedBarChartsData = new ArrayList<>();
    private Category category;

    public CombinedBarChart(List<BarChart> barCharts, Category category) {
        this.barCharts = barCharts;
        this.combinedBarChartsData = generateCombinedBarChartsData();
        this.category = category;
    }

    public List<BarChart> getBarCharts() {
        return barCharts;
    }

    public void setBarCharts(List<BarChart> barCharts) {
        this.barCharts = barCharts;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    private List<Map<String, Object>> generateCombinedBarChartsData() {
        logger.debug("Generating combined bar charts data...");
        List<Map<String, Object>> combinedBarChartsDatas = new ArrayList<>();
        for (BarChart barChart : barCharts) {
            Map<String, Object> data = new HashMap<>();
            LocalDate localDate = barChart.getLocalDate();
            data.put("date", localDate.toString());
            data.put("data", barChart.getDataByDate().get(localDate.toString()));
            combinedBarChartsDatas.add(data);
        }
        logger.debug("Finished generating combined bar charts data.");
        return combinedBarChartsDatas;
    }

    public List<Map<String, Object>> getCombinedBarChartsData() {
        return combinedBarChartsData;
    }
}
