package com.example.howscat.dto;

public class VomitAnalysisRequest {

    private String imageBase64;
    private String memo;
    private String imagePath;

    public VomitAnalysisRequest() {
    }

    public VomitAnalysisRequest(String imageBase64, String memo, String imagePath) {
        this.imageBase64 = imageBase64;
        this.memo = memo;
        this.imagePath = imagePath;
    }
}
