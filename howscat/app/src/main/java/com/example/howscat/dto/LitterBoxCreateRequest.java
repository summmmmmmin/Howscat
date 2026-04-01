package com.example.howscat.dto;

public class LitterBoxCreateRequest {
    private String date;
    private Integer count;
    private String color;
    private String shape;
    private Boolean abnormal;
    private String notes;

    public LitterBoxCreateRequest(String date, Integer count, String color,
                                   String shape, Boolean abnormal, String notes) {
        this.date = date;
        this.count = count;
        this.color = color;
        this.shape = shape;
        this.abnormal = abnormal;
        this.notes = notes;
    }

    public String getDate() { return date; }
    public Integer getCount() { return count; }
    public String getColor() { return color; }
    public String getShape() { return shape; }
    public Boolean getAbnormal() { return abnormal; }
    public String getNotes() { return notes; }
}
