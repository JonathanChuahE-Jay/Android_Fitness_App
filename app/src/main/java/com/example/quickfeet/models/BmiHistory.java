package com.example.quickfeet.models;

public class BmiHistory {
    private final int id;
    private final double bmi;
    private final double weight;
    private final double height;
    private final String date;

    public BmiHistory(int id, double bmi, double weight, double height, String date) {
        this.id = id;
        this.bmi = bmi;
        this.weight = weight;
        this.height = height;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public double getBmi() {
        return bmi;
    }

    public double getWeight() {
        return weight;
    }

    public double getHeight() {
        return height;
    }

    public String getDate() {
        return date;
    }
}
