package com.example.FixItNow.enums;

/** Payment lifecycle — mirrors Stripe PaymentIntent states. */
public enum PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REFUNDED
}

