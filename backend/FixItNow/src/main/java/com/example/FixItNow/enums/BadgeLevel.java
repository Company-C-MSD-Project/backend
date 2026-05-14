package com.example.FixItNow.enums;

/**
 * Reputation badge tiers for service providers (SRS §2.1.2 Reputation System).
 * Assigned automatically based on average rating and completed job count.
 */
public enum BadgeLevel {
    NONE,
    BRONZE,   // avg rating >= 3.0, >= 5  completed jobs
    SILVER,   // avg rating >= 3.5, >= 20 completed jobs
    GOLD,     // avg rating >= 4.0, >= 50 completed jobs
    TOP_RATED // avg rating >= 4.5, >= 100 completed jobs
}
