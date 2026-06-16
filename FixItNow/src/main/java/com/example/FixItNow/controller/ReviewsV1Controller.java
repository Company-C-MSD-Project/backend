package com.example.FixItNow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.v1.ReviewV1;
import com.example.FixItNow.service.ReviewV1Service;

import lombok.RequiredArgsConstructor;

/**
 * Root /reviews endpoints. Moderation (list/stats/flag/hide/resolve) is ADMIN-only;
 * reply is for the authenticated provider (services/reviews.ts, provider-reviews.ts).
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewsV1Controller {

    private final ReviewV1Service reviewService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReviewV1>> list() {
        return ResponseEntity.ok(reviewService.listAll());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(reviewService.stats());
    }

    @PostMapping("/{id}/flag")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> flag(@PathVariable Long id) {
        reviewService.flag(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> hide(@PathVariable Long id) {
        reviewService.hide(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resolve(@PathVariable Long id) {
        reviewService.resolve(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<Map<String, Object>> reply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object message = body.get("message");
        return ResponseEntity.ok(reviewService.reply(id, message != null ? String.valueOf(message) : null));
    }
}
