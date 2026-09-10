package com.reloop.exception;

/** Thrown when a requested item/pickup id doesn't exist in the database. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
