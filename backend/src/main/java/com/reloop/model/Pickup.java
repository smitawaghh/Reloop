package com.reloop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * COMPOSITION, not inheritance: a Pickup HAS-AN EwasteItem, it isn't a kind
 * of EwasteItem. Composition is the right relationship here because a
 * pickup request and a device are two different life cycles owned by two
 * different entities - deleting a pickup should never delete the device,
 * and one item could in principle have multiple past pickups.
 */
@Entity
public class Pickup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private EwasteItem item;

    /** The citizen who asked for this pickup. Nullable for the same reason EwasteItem.owner is. */
    @ManyToOne
    @JoinColumn(name = "requested_by_id")
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    private PickupStatus status = PickupStatus.PENDING;

    /** Copied from item.getPriorityWeight() at creation time so the queue can sort without loading every item. */
    private int priorityWeight;

    @ManyToOne
    @JoinColumn(name = "recycler_center_id")
    private RecyclerCenter recyclerCenter;

    private String pickupLocation;

    /** Set together, only once a recycler schedules the pickup (see PickupService.scheduleDatetime). */
    private LocalDate pickupDate;
    private LocalTime pickupTime;

    protected Pickup() {
    }

    public Pickup(EwasteItem item, String pickupLocation) {
        this.item = item;
        this.pickupLocation = pickupLocation;
        this.priorityWeight = item.getPriorityWeight();
    }

    public Long getId() {
        return id;
    }

    public EwasteItem getItem() {
        return item;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public PickupStatus getStatus() {
        return status;
    }

    public void setStatus(PickupStatus status) {
        this.status = status;
    }

    public int getPriorityWeight() {
        return priorityWeight;
    }

    public RecyclerCenter getRecyclerCenter() {
        return recyclerCenter;
    }

    public void setRecyclerCenter(RecyclerCenter recyclerCenter) {
        this.recyclerCenter = recyclerCenter;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

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
