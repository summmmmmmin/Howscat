package com.example.howscat.dto;

public class HealthScheduleUpdateRequest {
    private String nextDate; // yyyy-MM-dd
    private Integer customCycleMonth; // months
    private Boolean alarmEnabled;

    public HealthScheduleUpdateRequest() {
    }

    public HealthScheduleUpdateRequest(String nextDate, Integer customCycleMonth, Boolean alarmEnabled) {
        this.nextDate = nextDate;
        this.customCycleMonth = customCycleMonth;
        this.alarmEnabled = alarmEnabled;
    }

    public String getNextDate() {
        return nextDate;
    }

    public Integer getCustomCycleMonth() {
        return customCycleMonth;
    }

    public Boolean getAlarmEnabled() {
        return alarmEnabled;
    }
}

