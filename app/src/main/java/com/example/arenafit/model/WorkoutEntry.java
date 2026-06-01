package com.example.arenafit.model;

public class WorkoutEntry {
    public final String key;
    public final Workout workout;

    public WorkoutEntry(String key, Workout workout) {
        this.key = key;
        this.workout = workout;
    }
}
