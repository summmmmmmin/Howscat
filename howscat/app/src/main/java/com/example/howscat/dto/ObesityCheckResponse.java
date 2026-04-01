package com.example.howscat.dto;

public class ObesityCheckResponse {

    private String obesityLevel;
    private Double bodyFatPercent;
    private Double recommendedTargetWeight;
    private Double recommendedWater;
    private Double recommendedFood;

    public ObesityCheckResponse() {
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

