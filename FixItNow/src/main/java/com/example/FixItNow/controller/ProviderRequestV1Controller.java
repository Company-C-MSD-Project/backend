package com.example.FixItNow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.service.ProviderRequestV1Service;

import lombok.RequiredArgsConstructor;

/** Provider application review endpoints (services/provider-requests.ts). ADMIN-only. */
@RestController
@RequestMapping("/provider-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProviderRequestV1Controller {

    private final ProviderRequestV1Service service;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(service.stats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                            @RequestBody Map<String, Object> body) {
        String status = str(body, "status");
        String notes = str(body, "admin_notes", "adminNotes");
        return ResponseEntity.ok(service.updateStatus(id, status, notes));
    }

    private String str(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }
}
