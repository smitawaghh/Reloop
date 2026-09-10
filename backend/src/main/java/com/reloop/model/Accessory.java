package com.reloop.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Accessories (cables, chargers, headphones) have negligible recoverable
 * material value, so they get a small flat reward and are the lowest
 * pickup priority - there's no urgency or hazard in leaving them for last.
 */
@Entity
@DiscriminatorValue("ACCESSORY")
public class Accessory extends EwasteItem {

    private static final double BASE_REWARD = 20.0;

    protected Accessory() {
    }

    public Accessory(double weightKg, Condition condition, String location, String description) {
        super(weightKg, condition, location, description);
    }

    @Override
    public double calculateReward() {
        return BASE_REWARD * conditionMultiplier();
    }

    @Override
    public String getItemType() {
        return "Accessory";
    }

    @Override
    public int getPriorityWeight() {
        return 4; // LOW
    }
}
