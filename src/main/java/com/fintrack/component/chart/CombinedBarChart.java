package com.fintrack.component.chart;

import java.time.LocalDate;
import java.util.*;

import com.fintrack.constants.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombinedBarChart {

    private static final Logger logger = LoggerFactory.getLogger(CombinedBarChart.class);

    private List<BarChart> barCharts;
    List<Map<String, Object>> combinedBarChartsData = new ArrayList<>();
    private Map<String, String> subcategoryColorMap = new HashMap<>();
    private Map<String, String> assetNameColorMap = new HashMap<>();
    private String categoryName;

    private static final List<Color> COLORS = Color.getAllColors(); // Use the Color enum to get all colors

    private int colorIndex = 0;

    public CombinedBarChart(List<BarChart> barCharts, String categoryName) {
      this.barCharts = barCharts;
      this.combinedBarChartsData = generateCombinedBarChartsData();
      this.categoryName = categoryName;
      reassignColor();
  }

    public List<BarChart> getBarCharts() {
        return barCharts;
    }

    public void setBarCharts(List<BarChart> barCharts) {
        this.barCharts = barCharts;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    private void reassignColor() {
        logger.trace("Reassigning colors for category: " + categoryName);
        for (BarChart barChart : barCharts) {
            // Get or generate a color for the subcategory
            for(BarChartData barChartData : barChart.getBarChartDatas()) {
                if (categoryName.equals("None")) {
                    String assetName = barChartData.getAssetName();
                    if (!assetNameColorMap.containsKey(assetName)) {
                        String color = assignColor();
                        assetNameColorMap.put(assetName, color);
                    }
                    // Set the color for the bar chart data
                    barChartData.setColor(assetNameColorMap.get(assetName));
                }
                else{
                    String subcategory = barChartData.getSubcategory();
                    if (!subcategoryColorMap.containsKey(subcategory)) {
                        String color = assignColor();
                        subcategoryColorMap.put(subcategory, color);
                    }
                    // Set the color for the bar chart data
                    barChartData.setColor(subcategoryColorMap.get(subcategory));
                }
            }
            logger.trace("Bar Chart Date: " + barChart.getLocalDate());
            barChart.getBarChartDatas().forEach(
                barChartData -> {
                    logger.trace("Asset Name: " + barChartData.getAssetName() + ", Subcategory: " + barChartData.getSubcategory() + ", Color: " + barChartData.getColor());
                }
            );
            logger.trace("Subcategory Color Map: " + subcategoryColorMap);
            logger.trace("Asset Name Color Map: " + assetNameColorMap);
        }
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

    private String assignColor() {
      String color = COLORS.get(colorIndex).getHexCode(); // Get color from predefined list
      colorIndex = (colorIndex + 1) % COLORS.size(); // Cycle through colors
      return color; // Return color in hex format
    }
}
