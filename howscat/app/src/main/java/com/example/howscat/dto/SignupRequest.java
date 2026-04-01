package com.example.howscat.dto;

public class SignupRequest {

    private String loginId;
    private String password;
    private String name;

    public SignupRequest(String loginId, String password, String name) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
    }
    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }
}
