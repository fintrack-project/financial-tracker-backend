package com.fintrack.component;

public class ChartData implements Comparable<ChartData> {
    private String assetName;
    private String symbol;
    private String subcategory;
    private double value;
    private String color;
    private Integer priority;
    private Double percentage;

    public ChartData(String assetName, String symbol, String subcategory, double value, Integer priority, String color) {
        this.assetName = assetName;
        this.symbol = symbol;
        this.subcategory = subcategory;
        this.value = value;
        this.priority = priority;
        this.color = color;
    }

    public ChartData(String assetName, String symbol, String subcategory, double value, Integer priority, String color, Double percentage) {
        this.assetName = assetName;
        this.symbol = symbol;
        this.subcategory = subcategory;
        this.value = value;
        this.priority = priority;
        this.color = color;
        this.percentage = percentage;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    @Override
    public int compareTo(ChartData other) {
        // Sort by priority in ascending order (nulls last)
        if (this.priority == null && other.priority == null) {
            return 0;
        } else if (this.priority == null) {
            return 1;
        } else if (other.priority == null) {
            return -1;
        }
        return this.priority.compareTo(other.priority);
    }
}
