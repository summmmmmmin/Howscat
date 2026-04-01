package com.example.howscat.dto;

public class CalendarEventItem {

    private Long id;
    private String type;

    private String date;   // yyyy-MM-dd
    private String time;   // optional

    private String title;
    private String subtitle;
    private String imagePath;
    private String riskLevel;
    private String vomitColor;

    /** 토 분석 안내(vomit_status.guide_text) */
    private String guideText;

    /** 건강 일정과 같은 날·유형에 연결된 메모 */
    private String scheduleMemo;

    private Boolean alarmEnabled;

    /** 연결된 calendar_memo id (메모 수정 시) */
    private Long linkedMemoId;

    private Long healthTypeId;

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getVomitColor() {
        return vomitColor;
    }

    public String getGuideText() {
        return guideText;
    }

    public String getScheduleMemo() {
        return scheduleMemo;
    }

    public Boolean getAlarmEnabled() {
        return alarmEnabled;
    }

    public Long getLinkedMemoId() {
        return linkedMemoId;
    }

    public Long getHealthTypeId() {
        return healthTypeId;
    }
}
