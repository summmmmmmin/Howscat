package com.example.howscat.dto;

public class VetVisitItem {
    private Long visitId;
    private String date;
    private String hospitalName;
    private String diagnosis;
    private String prescription;
    private String notes;

    public Long getVisitId() { return visitId; }
    public String getDate() { return date; }
    public String getHospitalName() { return hospitalName; }
    public String getDiagnosis() { return diagnosis; }
    public String getPrescription() { return prescription; }
    public String getNotes() { return notes; }
}
