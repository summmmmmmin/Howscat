package com.example.howscat.dto;

public class CatRequest {

    private String name;
    private String gender;
    private String birthDate;

    public CatRequest(String name,
                      String gender,
                      String birthDate) {

        this.name = name;
        this.gender = gender;
        this.birthDate = birthDate;
    }
}
