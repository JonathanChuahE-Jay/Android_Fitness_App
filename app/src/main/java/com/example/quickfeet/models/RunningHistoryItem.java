package com.example.quickfeet.models;

public class RunningHistoryItem {
    private final int id;
    private final double distance;
    private final double speed;
    private final String time;
    private final double calories;

    public RunningHistoryItem(int id, double distance, double speed, String time, double calories) {
        this.id = id;
        this.distance = distance;
        this.speed = speed;
        this.time = time;
        this.calories = calories;
    }

    public int getId() {
        return id;
    }

    public double getDistance() {
        return distance;
    }

    public double getSpeed() {
        return speed;
    }

    public String getTime() {
        return time;
    }

    public double getCalories() {
        return calories;
    }
}
