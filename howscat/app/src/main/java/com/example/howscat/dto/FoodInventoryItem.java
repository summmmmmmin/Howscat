package com.example.howscat.dto;

public class FoodInventoryItem {
    private Long inventoryId;
    private String foodName;
    private String brand;
    private Double remainingGrams;
    private Double totalGrams;
    private String type;
    private String purchaseDate;
    private Boolean lowStockAlarm;

    public Long getInventoryId() { return inventoryId; }
    public String getFoodName() { return foodName; }
    public String getBrand() { return brand; }
    public Double getRemainingGrams() { return remainingGrams; }
    public Double getTotalGrams() { return totalGrams; }
    public String getType() { return type; }
    public String getPurchaseDate() { return purchaseDate; }
    public Boolean getLowStockAlarm() { return lowStockAlarm; }
}
