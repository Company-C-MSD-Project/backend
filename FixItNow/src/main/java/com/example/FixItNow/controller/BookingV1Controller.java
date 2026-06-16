package com.example.FixItNow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.v1.BookingStatusUpdateV1;
import com.example.FixItNow.dto.v1.BookingV1;
import com.example.FixItNow.dto.v1.CreateBookingRequestV1;
import com.example.FixItNow.service.BookingV1Service;
import com.example.FixItNow.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Booking endpoints for the /api/v1 contract (lib/booking.ts). Authenticated:
 * the homeowner creates bookings, the provider lists and advances them. Raw JSON
 * bodies (no ApiResponse envelope) to match the frontend fetch client.
 */
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingV1Controller {

    private final BookingV1Service bookingService;

    @PostMapping
    public ResponseEntity<BookingV1> create(@Valid @RequestBody CreateBookingRequestV1 req) {
        BookingV1 created = bookingService.create(req, SecurityUtil.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<BookingV1>> list(
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "provider_id", required = false) Long providerId,
            @RequestParam(name = "homeowner_id", required = false) Long homeownerId) {
        if (providerId != null || "provider".equalsIgnoreCase(role)) {
            Long pid = providerId != null ? providerId : SecurityUtil.getCurrentUserId();
            return ResponseEntity.ok(bookingService.listForProvider(pid));
        }
        Long hid = homeownerId != null ? homeownerId : SecurityUtil.getCurrentUserId();
        return ResponseEntity.ok(bookingService.listForHomeowner(hid));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingV1> updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody BookingStatusUpdateV1 req) {
        BookingV1 updated = bookingService.updateStatus(
                id, req.getStatus(), req.getReason(), SecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(updated);
    }
}
