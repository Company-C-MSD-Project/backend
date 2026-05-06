package com.example.FixItNow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import com.example.FixItNow.enums.BadgeLevel;

/**
 * Smart match result returned to homeowner — includes provider info,
 * distance, ETA, and estimated day rate (SRS §2.1.2 Smart Matching).
 */
@Data @Builder @AllArgsConstructor
public class ProviderMatchResponse {
    private Long providerId;
    private String name;
    private String serviceCategory;
    private Double rating;
    private BadgeLevel badgeLevel;
    private Double distanceKm;
    private Integer etaMinutes;
    private Double dayPaymentRate;
    private boolean isVerified;
}
