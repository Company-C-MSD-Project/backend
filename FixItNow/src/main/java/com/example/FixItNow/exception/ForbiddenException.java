package com.example.FixItNow.exception;

/**
 * Thrown when a resource operation is forbidden (e.g., user lacks permissions).
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
