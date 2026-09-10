package com.reloop.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Batteries require hazardous-material handling (safe discharge, chemical
 * containment) no matter how "worn" they are — a battery isn't really
 * "working" or "damaged" in the way a laptop is, it's either leaking/unsafe
 * or not. So reward intentionally ignores conditionMultiplier() and instead
 * pays a flat hazardous-handling fee plus a small per-kg component. This is
 * the clearest example of why calculateReward() has to be overridden per
 * subclass rather than shared in the base class.
 */
@Entity
@DiscriminatorValue("BATTERY")
public class Battery extends EwasteItem {

    private static final double HAZARD_HANDLING_FEE = 30.0;
    private static final double RATE_PER_KG = 10.0;

    protected Battery() {
    }

    public Battery(double weightKg, Condition condition, String location, String description) {
        super(weightKg, condition, location, description);
    }

    @Override
    public double calculateReward() {
        return HAZARD_HANDLING_FEE + (getWeightKg() * RATE_PER_KG);
    }

    @Override
    public String getItemType() {
        return "Battery";
    }

    @Override
    public int getPriorityWeight() {
        return 1; // HIGH - hazardous, collect first
    }
}
