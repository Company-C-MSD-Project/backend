package com.example.FixItNow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.FixItNow.service.BookingService;
import com.example.FixItNow.service.SmartMatchService;

/**
 * Booking lifecycle endpoints (SRS FR7, FR8).
 * Smart match: GET /api/bookings/match?category=&lat=&lng=
 * Create:  POST /api/bookings
 * Accept:  PUT  /api/bookings/{id}/accept
 * Start:   PUT  /api/bookings/{id}/start
 * Complete:PUT  /api/bookings/{id}/complete
 * Cancel:  PUT  /api/bookings/{id}/cancel
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final SmartMatchService smartMatchService;

    /** Smart provider matching — homeowner searches before booking (FR7). */
    @GetMapping("/match")
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<ApiResponse<List<ProviderMatchResponse>>> matchProviders(
            @RequestParam String category,
            @RequestParam double lat,
            @RequestParam double lng) {
        List<ProviderMatchResponse> matches = smartMatchService.findMatches(category, lat, lng);
        return ResponseEntity.ok(ApiResponse.ok("Providers found", matches));
    }

    @PostMapping
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<ApiResponse<Booking>> create(
            @RequestParam Long homeownerId,
            @Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.createBooking(homeownerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Booking created", booking));
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<ApiResponse<Booking>> accept(
            @PathVariable Long id,
            @RequestParam Long providerId) {
        return ResponseEntity.ok(ApiResponse.ok("Booking accepted", bookingService.acceptBooking(id, providerId)));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<ApiResponse<Booking>> start(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Job started", bookingService.startJob(id)));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<ApiResponse<Booking>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Job completed", bookingService.completeJob(id)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancel(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled", bookingService.cancelBooking(id, reason)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Booking>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Booking found", bookingService.getById(id)));
    }

    @GetMapping("/homeowner/{homeownerId}")
    public ResponseEntity<ApiResponse<List<Booking>>> getByHomeowner(@PathVariable Long homeownerId) {
        return ResponseEntity.ok(ApiResponse.ok("Bookings retrieved", bookingService.getByHomeowner(homeownerId)));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<ApiResponse<List<Booking>>> getByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(ApiResponse.ok("Bookings retrieved", bookingService.getByProvider(providerId)));
    }
}

