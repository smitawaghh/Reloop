package com.reloop.model;

/** Lifecycle states of a pickup request, in the order they normally occur. */
public enum PickupStatus {
    PENDING,
    ASSIGNED,
    SCHEDULED,
    COLLECTED,
    RECYCLED
}
