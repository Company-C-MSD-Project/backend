package com.example.FixItNow.dto.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/** Browse category shape (lib/booking.ts Category): {id, slug, name, icon, pros_count}. */
@Getter
@Builder
public class CategoryV1 {
    private final String id;
    private final String slug;
    private final String name;
    private final String icon;

    @JsonProperty("pros_count")
    private final long prosCount;
}
