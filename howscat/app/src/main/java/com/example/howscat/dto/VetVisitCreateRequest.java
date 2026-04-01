package com.example.howscat.dto;

public class VetVisitCreateRequest {
    private String date;
    private String hospitalName;
    private String diagnosis;
    private String prescription;
    private String notes;

    public VetVisitCreateRequest(String date, String hospitalName,
                                  String diagnosis, String prescription, String notes) {
        this.date = date;
        this.hospitalName = hospitalName;
        this.diagnosis = diagnosis;
        this.prescription = prescription;
        this.notes = notes;
    }

    public String getDate() { return date; }
    public String getHospitalName() { return hospitalName; }
    public String getDiagnosis() { return diagnosis; }
    public String getPrescription() { return prescription; }
    public String getNotes() { return notes; }
}
