package com.example.howscat.dto;

public class VomitAnalysisResponse {

    private Long vomitStatusId;
    private String severityLevel;
    private String guideText;
    private String riskLevel;
    private Boolean urgent;
    private String aiGuide;

    public VomitAnalysisResponse() {
    }

    public Long getVomitStatusId() { return vomitStatusId; }
    public String getSeverityLevel() { return severityLevel; }
    public String getGuideText() { return guideText; }
    public String getRiskLevel() { return riskLevel; }
    public Boolean getUrgent() { return urgent; }
    public String getAiGuide() { return aiGuide; }
}

