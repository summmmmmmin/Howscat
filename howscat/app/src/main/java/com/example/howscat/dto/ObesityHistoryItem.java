package com.example.howscat.dto;

public class ObesityHistoryItem {
    private String date; // yyyy-MM-dd
    private String obesityLevel;
    private Double bodyFatPercent;
    private Double recommendedTargetWeight;
    private Double recommendedWater;
    private Double recommendedFood;

    public ObesityHistoryItem() {
    }

    public String getDate() {
        return date;
    }

    public String getObesityLevel() {
        return obesityLevel;
    }

    public Double getBodyFatPercent() {
        return bodyFatPercent;
    }

    public Double getRecommendedTargetWeight() {
        return recommendedTargetWeight;
    }

    public Double getRecommendedWater() {
        return recommendedWater;
    }

    public Double getRecommendedFood() {
        return recommendedFood;
    }
}

