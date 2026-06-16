package com.example.FixItNow.dto.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/** Pending provider registration row (services/admin.ts ProviderRegistration). */
@Getter
@Builder
public class ProviderRegistrationV1 {
    private final String id;
    private final String initials;
    private final String name;
    private final String category;
    private final String location;

    @JsonProperty("submitted_at")
    private final String submittedAt;
}
