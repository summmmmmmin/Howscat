package com.example.howscat.dto;

public class CalendarMemoCreateRequest {

    private String content;
    private String memoDate; // yyyy-MM-dd
    private Long healthTypeId;

    public CalendarMemoCreateRequest() {
    }

    public CalendarMemoCreateRequest(String content, String memoDate) {
        this.content = content;
        this.memoDate = memoDate;
    }

    public CalendarMemoCreateRequest(String content, String memoDate, Long healthTypeId) {
        this.content = content;
        this.memoDate = memoDate;
        this.healthTypeId = healthTypeId;
    }

    public String getContent() {
        return content;
    }

    public String getMemoDate() {
        return memoDate;
    }

    public Long getHealthTypeId() {
        return healthTypeId;
    }

    public void setHealthTypeId(Long healthTypeId) {
        this.healthTypeId = healthTypeId;
    }
}
