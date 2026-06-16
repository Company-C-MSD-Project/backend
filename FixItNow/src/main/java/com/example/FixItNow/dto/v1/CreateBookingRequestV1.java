package com.example.FixItNow.dto.v1;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** POST /api/v1/bookings body (lib/booking.ts CreateBookingInput). */
@Getter @Setter
public class CreateBookingRequestV1 {

    @JsonProperty("homeowner_id")
    private String homeownerId;

    @JsonProperty("provider_id")
    private String providerId;

    /** Maps to the backend Service id. */
    @JsonProperty("sub_service_id")
    @NotBlank(message = "sub_service_id is required")
    private String subServiceId;

    @JsonProperty("service_name")
    private String serviceName;

    /** "on_the_spot" | "scheduled". */
    @JsonProperty("job_type")
    private String jobType;

    @JsonProperty("scheduled_date")
    private String scheduledDate;

    @JsonProperty("scheduled_time")
    private String scheduledTime;

    @JsonProperty("address_line")
    private String addressLine;

    private String district;

    @JsonProperty("postal_code")
    private String postalCode;

    private String landmarks;

    @JsonProperty("problem_desc")
    private String problemDesc;

    @JsonProperty("hourly_rate")
    private BigDecimal hourlyRate;

    @JsonProperty("est_hours")
    private Integer estHours;

    @JsonProperty("platform_fee")
    private BigDecimal platformFee;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;
}
