package com.example.FixItNow.controller;

import com.example.FixItNow.dto.response.ApiResponse;
import com.example.FixItNow.entity.Dispute;
import com.example.FixItNow.enums.DisputeStatus;
import com.example.FixItNow.service.DisputeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Dispute resolution endpoints (SRS §2.1.2 Dispute Resolution). */
@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeService disputeService;

    @PostMapping
    public ResponseEntity<ApiResponse<Dispute>> raise(
            @RequestParam Long bookingId,
            @RequestParam Long raisedById,
            @RequestParam String reason) {
        Dispute dispute = disputeService.raise(bookingId, raisedById, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Dispute raised", dispute));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Dispute>> resolve(
            @PathVariable Long id,
            @RequestParam Long adminId,
            @RequestParam String resolution) {
        return ResponseEntity.ok(ApiResponse.ok("Dispute resolved", disputeService.resolve(id, adminId, resolution)));
    }

    @PutMapping("/{id}/escalate")
    public ResponseEntity<ApiResponse<Dispute>> escalate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Dispute escalated", disputeService.escalate(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Dispute>>> getAll(
            @RequestParam(required = false) DisputeStatus status) {
        List<Dispute> disputes = (status != null)
                ? disputeService.getByStatus(status)
                : disputeService.getAll();
        return ResponseEntity.ok(ApiResponse.ok("Disputes retrieved", disputes));
    }
}
