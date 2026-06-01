package com.example.FixItNow.controller;

import com.example.FixItNow.dto.response.ApiResponse;
import com.example.FixItNow.entity.Service;
import com.example.FixItNow.service.ServiceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Service listing and management endpoints (SRS FR7). */
@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Service>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Services retrieved", serviceManagementService.getActiveServices()));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<Service>>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(ApiResponse.ok("Services retrieved", serviceManagementService.getByCategory(categoryId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Service>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Service found", serviceManagementService.getById(id)));
    }

    @PostMapping("/category/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Service>> create(@PathVariable Long categoryId,
                                                       @RequestBody Service service) {
        Service created = serviceManagementService.create(categoryId, service);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Service created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Service>> update(@PathVariable Long id, @RequestBody Service updates) {
        return ResponseEntity.ok(ApiResponse.ok("Service updated", serviceManagementService.update(id, updates)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        serviceManagementService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Service deactivated", null));
    }
}
