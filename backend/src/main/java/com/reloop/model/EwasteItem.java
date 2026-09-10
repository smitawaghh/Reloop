package com.reloop.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Abstract base for every device a user can register.
 *
 * ABSTRACTION: callers (the service layer) work against this type and never
 * need to know whether they're holding a Laptop, MobilePhone, Battery, or
 * Accessory. They just call calculateReward() and getPriorityWeight().
 *
 * ENCAPSULATION: fields are private; access goes through getters, and the
 * one setter with a real invariant (weight must be positive) validates it.
 *
 * INHERITANCE: Laptop, MobilePhone, Battery and Accessory extend this class
 * and share id/weight/condition/location/description for free. Pickup
 * lifecycle status (PENDING/ASSIGNED/.../RECYCLED) lives only on Pickup,
 * not here - a device's "status" only has meaning once a pickup exists for
 * it, so tracking it on both classes would just invite them to drift out
 * of sync.
 *
 * JPA: @Inheritance(SINGLE_TABLE) stores every subclass in one ewaste_item
 * table with a "item_type" discriminator column, instead of one table per
 * subclass. Simplest mapping strategy - fine here since all subclasses
 * share the same small set of columns and we never need to join across
 * subclass-only fields.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_type")
public abstract class EwasteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double weightKg;

    @Enumerated(EnumType.STRING)
    private Condition condition;

    private String location;
    private String description;

    /**
     * The citizen who registered this device. Nullable: there's no login,
     * so an item only has an owner when the frontend's demo-mode user
     * switcher supplied a userId at registration time.
     */
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    /** No-arg constructor required by JPA/Hibernate to reconstruct entities from DB rows. */
    protected EwasteItem() {
    }

    protected EwasteItem(double weightKg, Condition condition, String location, String description) {
        setWeightKg(weightKg);
        this.condition = condition;
        this.location = location;
        this.description = description;
    }

    /**
     * POLYMORPHISM: each subclass implements this differently because the
     * economics of reward genuinely differ per device type (see subclasses
     * for the reasoning behind each formula). Code that calls this method
     * (EwasteService) never branches on device type — it just calls
     * item.calculateReward() and gets the right answer for whatever
     * concrete type was passed in.
     */
    public abstract double calculateReward();

    /**
     * Getter alias so Jackson includes the computed reward in JSON
     * responses as "estimatedReward" - calculateReward() itself isn't
     * picked up by bean serialization since it doesn't start with "get".
     */
    public double getEstimatedReward() {
        return calculateReward();
    }

    /** Human-readable device type, used for API responses and UI labels. */
    public abstract String getItemType();

    /**
     * Lower number = handled with higher priority in the pickup queue.
     * Batteries are hazardous and must be collected first regardless of
     * how long they've been waiting; accessories are the least urgent.
     */
    public abstract int getPriorityWeight();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        if (weightKg <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0 kg");
        }
        this.weightKg = weightKg;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    /**
     * Shared condition multiplier: a working device is worth full reward,
     * a partially working one less, a damaged one least (it costs more to
     * refurbish/dispose of safely). Subclasses reuse this instead of each
     * re-implementing the same condition scaling.
     */
    protected double conditionMultiplier() {
        return switch (condition) {
            case WORKING -> 1.0;
            case PARTIALLY_WORKING -> 0.6;
            case DAMAGED -> 0.3;
        };
    }
}
