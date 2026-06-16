package com.example.FixItNow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.service.CategoryRequestV1Service;

import lombok.RequiredArgsConstructor;

/**
 * New-category request endpoints (services/category-requests.ts + services/admin.ts).
 * Reads/moderation are ADMIN; create is open to any authenticated user (a provider
 * requesting a new category). Supports both PATCH /{id} and POST /{id}/approve|reject.
 */
@RestController
@RequestMapping("/category-requests")
@RequiredArgsConstructor
public class CategoryRequestV1Controller {

    private final CategoryRequestV1Service service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.stats());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(payload));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> patch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.updateStatus(id, str(body, "status"), str(body, "admin_notes", "adminNotes")));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.updateStatus(id, "Active", null));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id) {
        return ResponseEntity.ok(service.updateStatus(id, "Rejected", null));
    }

    private String str(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }
}
