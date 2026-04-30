package com.example.howscat.dto;

public class MedicationCreateRequest {
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

    public MedicationCreateRequest(String name, String dosage, String frequency,
                                    String startDate, String endDate,
                                    Boolean alarmEnabled, Integer alarmHour, Integer alarmMinute,
                                    Integer alarmHour2, Integer alarmMinute2,
                                    String notes) {
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.alarmEnabled = alarmEnabled;
        this.alarmHour = alarmHour;
        this.alarmMinute = alarmMinute;
        this.alarmHour2 = alarmHour2;
        this.alarmMinute2 = alarmMinute2;
        this.notes = notes;
    }

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
