package com.example.FixItNow.exception;

/**
 * Thrown when authentication fails or token is invalid/expired.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
