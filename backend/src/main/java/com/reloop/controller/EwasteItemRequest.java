package com.reloop.controller;

/**
 * Plain request body for POST /api/items. Kept as a single simple class
 * (not a separate DTO/mapper layer) since the API only ever needs these
 * five fields and no translation logic beyond picking a subclass.
 */
public class EwasteItemRequest {
    private String itemType;       // "LAPTOP" | "MOBILE_PHONE" | "BATTERY" | "ACCESSORY"
    private double weightKg;
    private String condition;      // "WORKING" | "PARTIALLY_WORKING" | "DAMAGED"
    private String location;
    private String description;
    private Long userId; // optional - the citizen registering this item, if a demo user is selected

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
