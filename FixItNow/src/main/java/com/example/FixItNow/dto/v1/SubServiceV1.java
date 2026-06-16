package com.example.FixItNow.dto.v1;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * Browse sub-service shape (lib/booking.ts SubService):
 * {id, category_id, slug, name, description, icon, base_price}.
 * Maps to the backend Service entity.
 */
@Getter
@Builder
public class SubServiceV1 {
    private final String id;

    @JsonProperty("category_id")
    private final String categoryId;

    private final String slug;
    private final String name;
    private final String description;
    private final String icon;

    @JsonProperty("base_price")
    private final BigDecimal basePrice;
}
