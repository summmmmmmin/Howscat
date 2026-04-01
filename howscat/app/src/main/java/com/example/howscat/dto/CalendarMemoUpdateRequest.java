package com.example.howscat.dto;

public class CalendarMemoUpdateRequest {

    private String content;
    /** yyyy-MM-dd — 있으면 메모 날짜도 함께 변경 */
    private String memoDate;

    public CalendarMemoUpdateRequest() {
    }

    public CalendarMemoUpdateRequest(String content) {
        this.content = content;
    }

    public CalendarMemoUpdateRequest(String content, String memoDate) {
        this.content = content;
        this.memoDate = memoDate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMemoDate() {
        return memoDate;
    }

    public void setMemoDate(String memoDate) {
        this.memoDate = memoDate;
    }
}
