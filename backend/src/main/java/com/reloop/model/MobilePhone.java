package com.reloop.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Phones don't vary enough in weight to make weight-based pricing
 * meaningful (they're all roughly 150-250g), so reward is a flat base
 * amount adjusted by condition, rather than scaled by weight like a Laptop.
 */
@Entity
@DiscriminatorValue("MOBILE_PHONE")
public class MobilePhone extends EwasteItem {

    private static final double BASE_REWARD = 80.0;

    protected MobilePhone() {
    }

    public MobilePhone(double weightKg, Condition condition, String location, String description) {
        super(weightKg, condition, location, description);
    }

    @Override
    public double calculateReward() {
        return BASE_REWARD * conditionMultiplier();
    }

    @Override
    public String getItemType() {
        return "Mobile Phone";
    }

    @Override
    public int getPriorityWeight() {
        return 3; // NORMAL
    }
}
