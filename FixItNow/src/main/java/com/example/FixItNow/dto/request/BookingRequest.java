package com.example.FixItNow.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;s

import java.time.LocalDateTime;

/** Request body to create a new booking (SRS FR8 — real-time or scheduled). */
@Data
public class BookingRequest {

    @NotNull
    private Long serviceId;

    private Long providerId;        // optional — if null, smart match will assign one

    @NotNull @Future(message = "Scheduled date must be in the future")
    private LocalDateTime scheduledDate;

    private String description;     // homeowner describes the issue
    private String serviceType;
}