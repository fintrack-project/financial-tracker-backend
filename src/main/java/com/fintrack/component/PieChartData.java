package com.fintrack.component;

public class PieChartData {
    private String name;
    private double value;
    private String color;

    public PieChartData(String name, double value, String color) {
        this.name = name;
        this.value = value;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
