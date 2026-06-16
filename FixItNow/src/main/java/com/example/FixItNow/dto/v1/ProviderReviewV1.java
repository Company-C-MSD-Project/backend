package com.example.FixItNow.dto.v1;

import lombok.Builder;
import lombok.Getter;

/** Provider self-service review row (services/provider-reviews.ts ProviderReview). */
@Getter
@Builder
public class ProviderReviewV1 {
    private final String id;
    private final String name;
    private final String city;
    private final String service;
    private final String date;
    private final String paid;
    private final int rating;
    private final String text;
}
