package com.fintrack.component;

public class PieChartData extends ChartData {
    Double percentageOfSubcategory;

    public PieChartData(String assetName, String symbol, String subcategory, double value, Integer priority, String color) {
        super(assetName, symbol, subcategory, value, priority, color);
    }

    public PieChartData(String assetName, String symbol, String subcategory, double value, Integer priority, String color, Double percentage) {
        super(assetName, symbol, subcategory, value, priority, color, percentage);
    }

    public Double getPercentageOfSubcategory() {
        return percentageOfSubcategory;
    }

    public void setPercentageOfSubcategory(Double percentageOfSubcategory) {
        this.percentageOfSubcategory = percentageOfSubcategory;
    }
}
