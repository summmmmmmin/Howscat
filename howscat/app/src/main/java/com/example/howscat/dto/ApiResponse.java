package com.example.howscat.dto;

public class ApiResponse {

    private boolean success;
    private String message;
    private Integer userId;
    private Long catId;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Integer getUserId() { return userId; }

    public Long getCatId() { return catId; }
}
