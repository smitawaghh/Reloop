package com.reloop.exception;

/** Thrown when a pickup status change would skip a step or move backwards. */
public class InvalidPickupStatusTransitionException extends RuntimeException {
    public InvalidPickupStatusTransitionException(String message) {
        super(message);
    }
}
