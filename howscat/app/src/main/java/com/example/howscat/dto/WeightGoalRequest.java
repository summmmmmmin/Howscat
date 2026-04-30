package com.example.howscat.dto;

public class WeightGoalRequest {

    private final float weightGoal;

    public WeightGoalRequest(float weightGoal) {
        this.weightGoal = weightGoal;
    }

    public float getWeightGoal() {
        return weightGoal;
    }
}
