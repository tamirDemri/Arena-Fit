package com.example.arenafit.model;

public class Workout {
    public String type;
    public double value;
    public long timestamp;

    public Workout() {}

    public Workout(String type, double value, long timestamp) {
        this.type = type;
        this.value = value;
        this.timestamp = timestamp;
    }

    public static final String TYPE_PUSHUPS = "pushups";
    public static final String TYPE_PLANK = "plank";
    public static final String TYPE_RUNNING = "running";

    public static String displayName(String type) {
        if (type == null) return "Workout";
        switch (type) {
            case TYPE_PUSHUPS: return "Push Ups";
            case TYPE_PLANK: return "Plank";
            case TYPE_RUNNING: return "Running";
            default: return "Workout";
        }
    }

    public static String unit(String type) {
        if (type == null) return "";
        switch (type) {
            case TYPE_PUSHUPS: return "reps";
            case TYPE_PLANK: return "sec";
            case TYPE_RUNNING: return "km";
            default: return "";
        }
    }

    public static double weight(String type) {
        if (type == null) return 0;
        switch (type) {
            case TYPE_PUSHUPS: return 1.0;
            case TYPE_PLANK: return 1.0;
            case TYPE_RUNNING: return 100.0;
            default: return 0;
        }
    }
}
