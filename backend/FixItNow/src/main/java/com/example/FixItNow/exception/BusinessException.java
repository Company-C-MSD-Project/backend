package com.example.FixItNow.exception;

/**
 * Thrown when a business rule is violated during operations.
 * (e.g., invalid state transition, duplicate booking, etc.)
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
