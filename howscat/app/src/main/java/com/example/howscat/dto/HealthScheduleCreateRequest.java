package com.example.howscat.dto;

import com.google.gson.annotations.SerializedName;

public class HealthScheduleCreateRequest {

    @SerializedName("healthTypeId")
    private Long healthTypeId;

    @SerializedName("lastDate")
    private String lastDate;      // yyyy-MM-dd

    @SerializedName("alarmEnabled")
    private Boolean alarmEnabled;

    @SerializedName("customCycleMonth")
    private Integer customCycleMonth;

    public HealthScheduleCreateRequest(Long healthTypeId, String lastDate, Boolean alarmEnabled, Integer customCycleMonth) {
        this.healthTypeId = healthTypeId;
        this.lastDate = lastDate;
        this.alarmEnabled = alarmEnabled;
        this.customCycleMonth = customCycleMonth;
    }

    public Long getHealthTypeId() { return healthTypeId; }
    public String getLastDate() { return lastDate; }
    public Boolean getAlarmEnabled() { return alarmEnabled; }
    public Integer getCustomCycleMonth() { return customCycleMonth; }
}
