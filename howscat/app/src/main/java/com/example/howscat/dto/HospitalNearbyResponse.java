package com.example.howscat.dto;

public class HospitalNearbyResponse {

    private Long id;
    private String kakaoPlaceId;

    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phone;

    private Boolean open24Hours;
    private Boolean operating;
    private Double rating;

    private Double distanceKm;
    private Boolean favorited;

    /** 카카오 장소 페이지 URL */
    private String placeUrl;

    public Long getId() {
        return id;
    }

    public String getKakaoPlaceId() {
        return kakaoPlaceId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getPhone() {
        return phone;
    }

    public Boolean getOpen24Hours() {
        return open24Hours;
    }

    public Boolean getOperating() {
        return operating;
    }

    public Double getRating() {
        return rating;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public Boolean getFavorited() {
        return favorited;
    }

    public String getPlaceUrl() {
        return placeUrl;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setKakaoPlaceId(String kakaoPlaceId) {
        this.kakaoPlaceId = kakaoPlaceId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setOpen24Hours(Boolean open24Hours) {
        this.open24Hours = open24Hours;
    }

    public void setOperating(Boolean operating) {
        this.operating = operating;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public void setPlaceUrl(String placeUrl) {
        this.placeUrl = placeUrl;
    }

    // UI에서 찜 토글을 위해 필요
    public void setFavorited(Boolean favorited) {
        this.favorited = favorited;
    }
}

