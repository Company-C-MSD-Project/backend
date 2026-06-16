package com.example.FixItNow.dto.v1;

import lombok.Builder;
import lombok.Getter;

/** Admin provider-list row (services/providers.ts Provider). */
@Getter
@Builder
public class ProviderAdminV1 {
    private final String id;
    private final String initials;
    private final String name;
    private final String email;
    private final String category;
    private final String icon;
    private final String district;
    private final long jobs;
    private final double rating;
    /** "Active" | "Suspended" | "New" | "Top". */
    private final String status;
}
