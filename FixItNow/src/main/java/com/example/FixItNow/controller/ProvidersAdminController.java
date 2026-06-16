package com.example.FixItNow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.v1.ProviderAdminV1;
import com.example.FixItNow.dto.v1.ProviderRegistrationV1;
import com.example.FixItNow.dto.v1.ProviderReviewV1;
import com.example.FixItNow.service.AdminV1Service;
import com.example.FixItNow.service.ProviderSelfV1Service;
import com.example.FixItNow.service.ReviewV1Service;
import com.example.FixItNow.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * Root /providers endpoints. Admin management (list/stats/pending/approve/reject/
 * suspend/reinstate) is ADMIN-only; provider self-service (/me/*) is for the
 * authenticated provider. Browse (/api/v1/providers) is a separate public controller.
 */
@RestController
@RequestMapping("/providers")
@RequiredArgsConstructor
public class ProvidersAdminController {

    private final AdminV1Service adminService;
    private final ProviderSelfV1Service providerSelfService;
    private final ReviewV1Service reviewService;

    // ----- admin -----

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProviderAdminV1>> list() {
        return ResponseEntity.ok(adminService.listProviders());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(adminService.providerStats());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProviderRegistrationV1>> pending() {
        return ResponseEntity.ok(adminService.pendingProviders());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id) {
        adminService.approveProvider(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id) {
        adminService.rejectProvider(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> suspend(@PathVariable Long id) {
        adminService.suspendProvider(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/{id}/reinstate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reinstate(@PathVariable Long id) {
        adminService.reinstateProvider(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ----- provider self-service -----

    @GetMapping("/me/stats")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<Map<String, Object>> myStats() {
        return ResponseEntity.ok(providerSelfService.myStats(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/me/reviews")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<List<ProviderReviewV1>> myReviews() {
        return ResponseEntity.ok(reviewService.myReviews(SecurityUtil.getCurrentUserId()));
    }

    @GetMapping("/me/reviews/summary")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<Map<String, Object>> myReviewsSummary() {
        return ResponseEntity.ok(reviewService.mySummary(SecurityUtil.getCurrentUserId()));
    }
}
