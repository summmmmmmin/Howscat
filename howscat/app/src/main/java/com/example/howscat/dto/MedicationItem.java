package com.example.howscat.dto;

public class MedicationItem {
    private Long medicationId;
    private String name;
    private String dosage;
    private String frequency;
    private String startDate;
    private String endDate;
    private Boolean alarmEnabled;
    private Integer alarmHour;
    private Integer alarmMinute;
    private Integer alarmHour2;
    private Integer alarmMinute2;
    private String notes;

    public Long getMedicationId() { return medicationId; }
    public String getName() { return name; }
    public String getDosage() { return dosage; }
    public String getFrequency() { return frequency; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public Boolean getAlarmEnabled() { return alarmEnabled; }
    public Integer getAlarmHour() { return alarmHour; }
    public Integer getAlarmMinute() { return alarmMinute; }
    public Integer getAlarmHour2() { return alarmHour2; }
    public Integer getAlarmMinute2() { return alarmMinute2; }
    public String getNotes() { return notes; }
}
