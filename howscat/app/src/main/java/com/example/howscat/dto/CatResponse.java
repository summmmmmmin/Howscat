package com.example.howscat.dto;

public class CatResponse {

    private Long id;
    private String name;
    private Integer age;
    private String breed;
    private String obesityStatus;
    private String vomitingSeverity;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public String getBreed() {
        return breed;
    }

    public String getObesityStatus() {
        return obesityStatus;
    }

    public String getVomitingSeverity() {
        return vomitingSeverity;
    }
}