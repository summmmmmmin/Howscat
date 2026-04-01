package com.example.howscat.dto;

public class ObesityCheckRequest {

    private Double bodyFatPercent;
    private Double weightKg;
    private Double feedKcalPerG;

    public ObesityCheckRequest() {
    }

    public ObesityCheckRequest(Double bodyFatPercent, Double weightKg, Double feedKcalPerG) {
        this.bodyFatPercent = bodyFatPercent;
        this.weightKg = weightKg;
        this.feedKcalPerG = feedKcalPerG;
    }
}

