package com.fintrack.component;

public class PieChartData {
    private String assetName;
    private String symbol;
    private String subcategory;
    private double value;
    private String color;

    public PieChartData(String assetName, String symbol, String subcategory, double value, String color) {
        this.assetName = assetName;
        this.symbol = symbol;
        this.subcategory = subcategory;
        this.value = value;
        this.color = color;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return "PieChart{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", color='" + color + '\'' +
                '}';
    } 
}
