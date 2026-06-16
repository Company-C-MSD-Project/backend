package com.example.FixItNow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.FixItNow.service.AdminV1Service;

import lombok.RequiredArgsConstructor;

/** Admin dashboard endpoints (services/admin.ts): /dashboard/overview, /dashboard/metrics, /activity. */
@RestController
@RequiredArgsConstructor
public class DashboardV1Controller {

    private final AdminV1Service adminService;

    @GetMapping("/dashboard/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(adminService.overview());
    }

    @GetMapping("/dashboard/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> metrics() {
        return ResponseEntity.ok(adminService.metrics());
    }

    @GetMapping("/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> activity() {
        return ResponseEntity.ok(adminService.activity());
    }
}
