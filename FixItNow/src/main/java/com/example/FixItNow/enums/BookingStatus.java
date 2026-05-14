package com.example.FixItNow.enums;

/**
 * Booking lifecycle states as defined in the SRS State-Transition Diagram (§2.1.2).
 * Transitions: PENDING → ACCEPTED → IN_PROGRESS → COMPLETED
 *              PENDING or ACCEPTED → CANCELLED
 */
public enum BookingStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}