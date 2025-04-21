package com.fintrack.component;

import org.springframework.cglib.core.Local;

import java.time.LocalDate;

public class BarChartData extends ChartData {
      Double percentageOfSubcategory;
      LocalDate localDate;

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

      public LocalDate getLocalDate() {
          return localDate;
      }

      public void setLocalDate(LocalDate localDate) {
          this.localDate = localDate;
      }
}
