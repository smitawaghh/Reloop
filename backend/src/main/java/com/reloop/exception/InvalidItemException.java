package com.reloop.exception;

/** Thrown when a client submits an item type, weight, or condition we can't accept. */
public class InvalidItemException extends RuntimeException {
    public InvalidItemException(String message) {
        super(message);
    }
}
