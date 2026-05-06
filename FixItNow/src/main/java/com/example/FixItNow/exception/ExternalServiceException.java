package com.example.FixItNow.exception;

/**
 * Thrown when external service calls fail (Stripe, Google Maps, Email, etc.).
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
