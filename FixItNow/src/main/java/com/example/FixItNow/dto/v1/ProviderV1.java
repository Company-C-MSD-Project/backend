package com.example.FixItNow.dto.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * Browse provider shape (lib/booking.ts Provider). Mapped from a SERVICE_PROVIDER
 * User. Fields the JPA model does not yet carry (hourly_rate, years_experience,
 * city, distance_km, headline, bio) are defaulted; the frontend tolerates them.
 */
@Getter
@Builder
public class ProviderV1 {
    private final String id;
    private final String headline;

    @JsonProperty("category_id")
    private final String categoryId;

    private final double rating;

    @JsonProperty("jobs_done")
    private final long jobsDone;

    @JsonProperty("years_experience")
    private final int yearsExperience;

    @JsonProperty("hourly_rate")
    private final double hourlyRate;

    private final String city;

    @JsonProperty("distance_km")
    private final Double distanceKm;

    private final boolean verified;

    @JsonProperty("top_rated")
    private final boolean topRated;

    private final boolean available;
    private final String bio;
    private final ProfileV1 profile;

    /** Nested profile (lib/booking.ts Provider.profile). */
    @Getter
    @Builder
    public static class ProfileV1 {
        private final String id;

        @JsonProperty("display_name")
        private final String displayName;

        private final String username;

        @JsonProperty("avatar_url")
        private final String avatarUrl;
    }
}
