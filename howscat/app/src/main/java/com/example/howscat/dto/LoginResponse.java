package com.example.howscat.dto;

public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String name;
    private Long lastViewedCatId;
    private String lastViewedCatName;
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }

    public String getName() { return name; }
    public Long getLastViewedCatId() { return lastViewedCatId; }
    public String getLastViewedCatName() { return lastViewedCatName; }
}
