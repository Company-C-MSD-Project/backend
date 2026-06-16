package com.example.FixItNow.dto.v1;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** PATCH /api/v1/bookings/{id}/status body: { "status": "...", "reason"?: "..." }. */
@Getter @Setter
public class BookingStatusUpdateV1 {

    @NotBlank(message = "status is required")
    private String status;

    /** Optional cancellation reason. */
    private String reason;
}
