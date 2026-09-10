package com.reloop.exception;

/** Thrown when a pickup is requested for an item that already has a non-recycled pickup in progress. */
public class ActivePickupExistsException extends RuntimeException {
    public ActivePickupExistsException(String message) {
        super(message);
    }
}
