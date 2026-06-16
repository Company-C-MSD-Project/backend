package com.example.FixItNow.dto.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/** Admin homeowner-list row (services/homeowners.ts Homeowner). */
@Getter
@Builder
public class HomeownerAdminV1 {
    private final String id;
    private final String initials;
    private final String name;
    private final String email;
    private final String location;
    private final long bookings;
    private final String spent;

    @JsonProperty("member_since")
    private final String memberSince;

    /** "Active" | "New" | "Flagged". */
    private final String status;
}
