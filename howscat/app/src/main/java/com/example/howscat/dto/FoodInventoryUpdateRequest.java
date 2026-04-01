package com.example.howscat.dto;

public class FoodInventoryUpdateRequest {
    private String foodName;
    private String brand;
    private Double remainingGrams;
    private Double totalGrams;
    private String type;
    private String purchaseDate;
    private Boolean lowStockAlarm;

    public FoodInventoryUpdateRequest(String foodName, String brand,
                                       Double remainingGrams, Double totalGrams,
                                       String type, String purchaseDate, Boolean lowStockAlarm) {
        this.foodName = foodName;
        this.brand = brand;
        this.remainingGrams = remainingGrams;
        this.totalGrams = totalGrams;
        this.type = type;
        this.purchaseDate = purchaseDate;
        this.lowStockAlarm = lowStockAlarm;
    }

    public String getFoodName() { return foodName; }
    public String getBrand() { return brand; }
    public Double getRemainingGrams() { return remainingGrams; }
    public Double getTotalGrams() { return totalGrams; }
    public String getType() { return type; }
    public String getPurchaseDate() { return purchaseDate; }
    public Boolean getLowStockAlarm() { return lowStockAlarm; }
}
