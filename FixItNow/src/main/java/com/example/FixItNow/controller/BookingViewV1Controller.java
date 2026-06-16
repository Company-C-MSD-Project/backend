package com.example.FixItNow.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.service.BookingViewV1Service;
import com.example.FixItNow.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * Homeowner dashboard booking views (services/bookings.ts) at root paths:
 * GET /bookings/active, GET /bookings/past, PATCH /bookings/{id}/status.
 * Distinct from /api/v1/bookings (the fetch-client booking flow). Authenticated.
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingViewV1Controller {

    private final BookingViewV1Service bookingViewService;

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> active() {
        return ResponseEntity.ok(bookingViewService.active(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/past")
    public ResponseEntity<Map<String, Object>> past() {
        return ResponseEntity.ok(bookingViewService.past(SecurityUtil.getCurrentUserId()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        bookingViewService.updateStatus(id, body);
        return ResponseEntity.noContent().build();
    }
}
