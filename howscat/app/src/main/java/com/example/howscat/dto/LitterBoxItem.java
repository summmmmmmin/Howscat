package com.example.howscat.dto;

public class LitterBoxItem {
    private Long recordId;
    private String date;
    private Integer count;
    private String color;
    private String shape;
    private Boolean abnormal;
    private String notes;

    public Long getRecordId() { return recordId; }
    public String getDate() { return date; }
    public Integer getCount() { return count; }
    public String getColor() { return color; }
    public String getShape() { return shape; }
    public Boolean getAbnormal() { return abnormal; }
    public String getNotes() { return notes; }
}
