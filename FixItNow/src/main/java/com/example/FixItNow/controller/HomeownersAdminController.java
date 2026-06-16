package com.example.FixItNow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.v1.HomeownerAdminV1;
import com.example.FixItNow.service.AdminV1Service;

import lombok.RequiredArgsConstructor;

/** Root /homeowners admin endpoints (services/homeowners.ts). ADMIN-only. */
@RestController
@RequestMapping("/homeowners")
@RequiredArgsConstructor
public class HomeownersAdminController {

    private final AdminV1Service adminService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HomeownerAdminV1>> list() {
        return ResponseEntity.ok(adminService.listHomeowners());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(adminService.homeownerStats());
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> suspend(@PathVariable Long id) {
        adminService.suspendHomeowner(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
