package com.example.FixItNow.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.FixItNow.service.SystemMonitorV1Service;

import lombok.RequiredArgsConstructor;

/**
 * Admin security overview (services/security.ts) and system health (services/system.ts):
 * /security/events, /security/scan, /system-health. ADMIN-only.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemMonitorV1Controller {

    private final SystemMonitorV1Service monitorService;

    @GetMapping("/security/events")
    public ResponseEntity<Map<String, Object>> securityEvents() {
        return ResponseEntity.ok(monitorService.securityOverview());
    }

    @PostMapping("/security/scan")
    public ResponseEntity<Map<String, Object>> securityScan() {
        return ResponseEntity.ok(monitorService.runScan());
    }

    @GetMapping("/system-health")
    public ResponseEntity<Map<String, Object>> systemHealth() {
        return ResponseEntity.ok(monitorService.systemHealth());
    }
}
