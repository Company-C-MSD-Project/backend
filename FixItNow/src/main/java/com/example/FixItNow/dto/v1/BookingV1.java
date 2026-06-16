package com.example.FixItNow.dto.v1;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * Booking shape consumed by the frontend (lib/booking.ts Booking). Mapped from the
 * backend Booking entity; fields the entity does not carry (district, postal_code,
 * landmarks, est_hours, platform_fee) are defaulted.
 */
@Getter
@Builder
public class BookingV1 {
    private final String id;

    @JsonProperty("ref_code")
    private final String refCode;

    @JsonProperty("homeowner_id")
    private final String homeownerId;

    @JsonProperty("provider_id")
    private final String providerId;

    @JsonProperty("service_name")
    private final String serviceName;

    @JsonProperty("job_type")
    private final String jobType;

    @JsonProperty("scheduled_date")
    private final String scheduledDate;

    @JsonProperty("scheduled_time")
    private final String scheduledTime;

    @JsonProperty("address_line")
    private final String addressLine;

    private final String district;

    @JsonProperty("postal_code")
    private final String postalCode;

    private final String landmarks;

    @JsonProperty("problem_desc")
    private final String problemDesc;

    private final String status;

    @JsonProperty("hourly_rate")
    private final BigDecimal hourlyRate;

    @JsonProperty("est_hours")
    private final int estHours;

    @JsonProperty("platform_fee")
    private final BigDecimal platformFee;

    @JsonProperty("total_amount")
    private final BigDecimal totalAmount;

    @JsonProperty("created_at")
    private final String createdAt;
}
