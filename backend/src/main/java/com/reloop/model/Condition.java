package com.reloop.model;

/**
 * Physical condition of a registered device.
 * An enum is used instead of a raw String so invalid values (e.g. "brokn")
 * are rejected at compile time / deserialization time rather than causing
 * silent bugs in reward calculations.
 */
public enum Condition {
    WORKING,
    PARTIALLY_WORKING,
    DAMAGED
}
