package com.example.howscat.dto;

public class CareWeightRequest {

    private Double weightKg;
    private Double waterMl;
    private Double foodG;
    private String memoDate;

    public CareWeightRequest() {
    }

    public CareWeightRequest(Double weightKg, Double waterMl, Double foodG, String memoDate) {
        this.weightKg = weightKg;
        this.waterMl = waterMl;
        this.foodG = foodG;
        this.memoDate = memoDate;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public Double getWaterMl() {
        return waterMl;
    }

    public Double getFoodG() {
        return foodG;
    }

    public String getMemoDate() {
        return memoDate;
    }
}
