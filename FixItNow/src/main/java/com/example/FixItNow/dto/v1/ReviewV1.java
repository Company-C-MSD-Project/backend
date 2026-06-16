package com.example.FixItNow.dto.v1;

import lombok.Builder;
import lombok.Getter;

/** Admin review-moderation row (services/reviews.ts Review). */
@Getter
@Builder
public class ReviewV1 {
    private final String id;
    private final String initials;
    private final String name;
    private final String city;
    private final String date;
    private final String text;
    private final String service;
    private final String pro;
    private final String amount;
    private final int rating;
    private final boolean flagged;
}
