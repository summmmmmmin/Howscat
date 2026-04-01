package com.example.howscat.dto;

public class WeightHistoryItem {
    private String date;   // yyyy-MM-dd
    private Double weightKg;
    private Double recommendedWaterMl;
    private Double recommendedFoodG;

    public WeightHistoryItem() {
    }

    public String getDate() {
        return date;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public Double getRecommendedWaterMl() {
        return recommendedWaterMl;
    }

    public Double getRecommendedFoodG() {
        return recommendedFoodG;
    }
}

