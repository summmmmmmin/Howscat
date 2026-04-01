package com.example.howscat.dto;

public class HealthScheduleItem {
    private Long healthScheduleId;
    private Long healthTypeId;
    private String healthTypeName;
    private String catName;
    private String nextDate; // yyyy-MM-dd
    private Integer effectiveCycleMonth;
    private Boolean alarmEnabled;
    private Integer customCycleMonth;

    public Long getHealthScheduleId() {
        return healthScheduleId;
    }

    public Long getHealthTypeId() {
        return healthTypeId;
    }

    public String getHealthTypeName() {
        return healthTypeName;
    }

    public String getCatName() {
        return catName;
    }

    public String getNextDate() {
        return nextDate;
    }

    public Integer getEffectiveCycleMonth() {
        return effectiveCycleMonth;
    }

    public Boolean getAlarmEnabled() {
        return alarmEnabled;
    }

    public Integer getCustomCycleMonth() {
        return customCycleMonth;
    }
}

