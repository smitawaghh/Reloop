package com.reloop.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Pure unit tests for the polymorphic calculateReward() implementations - no Spring context needed. */
class RewardCalculationTest {

    @Test
    void laptopRewardScalesWithWeightAndCondition() {
        Laptop workingLaptop = new Laptop(2.5, Condition.WORKING, "Home", "Dell");
        Laptop damagedLaptop = new Laptop(2.5, Condition.DAMAGED, "Home", "Dell");

        assertThat(workingLaptop.calculateReward()).isEqualTo(100.0); // 2.5 * 40 * 1.0
        assertThat(damagedLaptop.calculateReward()).isEqualTo(30.0);  // 2.5 * 40 * 0.3
    }

    @Test
    void mobilePhoneRewardIsFlatRateAdjustedByCondition() {
        MobilePhone workingPhone = new MobilePhone(0.2, Condition.WORKING, "Home", "iPhone");
        MobilePhone partialPhone = new MobilePhone(0.2, Condition.PARTIALLY_WORKING, "Home", "iPhone");

        assertThat(workingPhone.calculateReward()).isEqualTo(80.0);  // 80 * 1.0
        assertThat(partialPhone.calculateReward()).isEqualTo(48.0); // 80 * 0.6
    }

    @Test
    void batteryRewardIgnoresConditionAndPaysHazardFeePlusWeight() {
        Battery workingBattery = new Battery(1.0, Condition.WORKING, "Home", "Power bank");
        Battery damagedBattery = new Battery(1.0, Condition.DAMAGED, "Home", "Power bank");

        // 30 (hazard fee) + 1.0 * 10 (rate/kg) = 40, regardless of condition.
        assertThat(workingBattery.calculateReward()).isEqualTo(40.0);
        assertThat(damagedBattery.calculateReward()).isEqualTo(40.0);
    }

    @Test
    void accessoryRewardIsFlatRateAdjustedByCondition() {
        Accessory workingAccessory = new Accessory(0.2, Condition.WORKING, "Home", "Headphones");
        Accessory damagedAccessory = new Accessory(0.2, Condition.DAMAGED, "Home", "Headphones");

        assertThat(workingAccessory.calculateReward()).isEqualTo(20.0); // 20 * 1.0
        assertThat(damagedAccessory.calculateReward()).isEqualTo(6.0);  // 20 * 0.3
    }

    @Test
    void negativeOrZeroWeightIsRejectedAtConstructionTime() {
        assertThatThrownBy(() -> new Laptop(0, Condition.WORKING, "Home", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Laptop(-1.5, Condition.WORKING, "Home", "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
