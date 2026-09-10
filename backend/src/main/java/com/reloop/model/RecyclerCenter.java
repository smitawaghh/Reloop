package com.reloop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * A place that can be assigned to collect a pickup. Deliberately simple -
 * just a name and location. No capacity, availability, distance, or
 * assignment-algorithm modeling; a recycler picks one from a plain list.
 */
@Entity
public class RecyclerCenter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;

    protected RecyclerCenter() {
    }

    public RecyclerCenter(String name, String location) {
        this.name = name;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
