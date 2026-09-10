package com.reloop.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Laptops are the highest-value recyclable device by weight (aluminum,
 * copper, rare-earth metals in the board), so reward scales directly with
 * weight and condition: a heavier, working laptop is worth meaningfully
 * more to recycle than a light, damaged one.
 */
@Entity
@DiscriminatorValue("LAPTOP")
public class Laptop extends EwasteItem {

    private static final double RATE_PER_KG = 40.0;

    protected Laptop() {
    }

    public Laptop(double weightKg, Condition condition, String location, String description) {
        super(weightKg, condition, location, description);
    }

    @Override
    public double calculateReward() {
        return getWeightKg() * RATE_PER_KG * conditionMultiplier();
    }

    @Override
    public String getItemType() {
        return "Laptop";
    }

    @Override
    public int getPriorityWeight() {
        return 2; // MEDIUM
    }
}
