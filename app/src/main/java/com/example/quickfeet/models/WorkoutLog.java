package com.example.quickfeet.models;

public class WorkoutLog {
    private int logId;
    private String workoutName;
    private int sets;
    private int reps;
    private double weight;

    public WorkoutLog(int logId, String workoutName, int sets, int reps, double weight) {
        this.logId = logId;
        this.workoutName = workoutName;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
    }

    public int getLogId() {
        return logId;
    }

    public String getWorkoutName() {
        return workoutName;
    }

    public int getSets() {
        return sets;
    }

    public int getReps() {
        return reps;
    }

    public double getWeight() {
        return weight;
    }
}
