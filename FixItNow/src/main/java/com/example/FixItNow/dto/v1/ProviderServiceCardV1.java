package com.example.FixItNow.dto.v1;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * Provider service-card response (ProviderServiceCardsPage / services/services.ts
 * ServiceCard), snake_case to match the frontend contract exactly.
 */
@Getter
@Builder
public class ProviderServiceCardV1 {
    private final String id;
    private final String title;
    private final String category;

    @JsonProperty("rate_type")
    private final String rateType;

    @JsonProperty("rate_amount")
    private final BigDecimal rateAmount;

    @JsonProperty("min_fee")
    private final BigDecimal minFee;

    @JsonProperty("short_summary")
    private final String shortSummary;

    @JsonProperty("full_description")
    private final String fullDescription;

    private final String duration;
    private final String warranty;

    @JsonProperty("cover_image")
    private final String coverImage;

    private final String status;
    private final List<String> districts;
    private final String emoji;
    private final String bg;

    @JsonProperty("price_line")
    private final String priceLine;

    private final boolean published;
}
