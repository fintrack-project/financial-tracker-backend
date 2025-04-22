package com.fintrack.component.chart;

import java.time.LocalDate;

public class BarChartData extends ChartData {
        Double percentageOfSubcategory;

        public BarChartData(ChartData chartData) {
            super(
                chartData.getAssetName(), 
                chartData.getSymbol(), 
                chartData.getSubcategory(), 
                chartData.getValue(), 
                chartData.getPriority(), 
                chartData.getColor(), 
                chartData.getPercentage()
            );
        }

        public BarChartData(String assetName, String symbol, String subcategory, double value, Integer priority, String color) {
            super(assetName, symbol, subcategory, value, priority, color);
        }

        public BarChartData(String assetName, String symbol, String subcategory, double value, Integer priority, String color, Double percentage) {
            super(assetName, symbol, subcategory, value, priority, color, percentage);
        }

        public Double getPercentageOfSubcategory() {
            return percentageOfSubcategory;
        }

        public void setPercentageOfSubcategory(Double percentageOfSubcategory) {
            this.percentageOfSubcategory = percentageOfSubcategory;
        }
}
