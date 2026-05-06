package com.example.FixItNow.exception;

/** Thrown for business-rule violations (duplicate email, invalid state transitions) — HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
