package com.reloop.controller;

import java.time.LocalDate;
import java.time.LocalTime;

public class SchedulePickupRequest {
    private LocalDate pickupDate;
    private LocalTime pickupTime;

    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }

    public LocalTime getPickupTime() {
        return pickupTime;
    }

    public void setPickupTime(LocalTime pickupTime) {
        this.pickupTime = pickupTime;
    }
}
