package com.example.FixItNow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.v1.ProviderServiceCardV1;
import com.example.FixItNow.service.ProviderServiceV1Service;
import com.example.FixItNow.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * Provider "My Service Cards" CRUD (frontend/src/services/services.ts servicesService,
 * used by ProviderServiceCardsPage). Root-level /services — no /api prefix, matching the
 * frontend's apiBaseUrl()/axios clients which send paths verbatim (see e.g. /provider-requests,
 * /category-requests). Distinct from the legacy /api/services catalog, which is untouched.
 * SERVICE_PROVIDER-only; every card read/write is scoped to the authenticated provider.
 */
@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SERVICE_PROVIDER')")
public class ProviderServiceV1Controller {

    private final ProviderServiceV1Service service;

    @GetMapping
    public ResponseEntity<List<ProviderServiceCardV1>> list() {
        return ResponseEntity.ok(service.list(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping
    public ResponseEntity<ProviderServiceCardV1> create(@RequestBody Map<String, Object> payload) {
        ProviderServiceCardV1 created = service.create(SecurityUtil.getCurrentUserId(), payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Also handles togglePublish, which PATCHes just {published, status}. */
    @PatchMapping("/{id}")
    public ResponseEntity<ProviderServiceCardV1> update(@PathVariable Long id,
                                                         @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(service.update(SecurityUtil.getCurrentUserId(), id, payload));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        service.delete(SecurityUtil.getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
