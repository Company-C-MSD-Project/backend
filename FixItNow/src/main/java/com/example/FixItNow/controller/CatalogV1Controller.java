package com.example.FixItNow.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.v1.CategoryV1;
import com.example.FixItNow.dto.v1.ProviderV1;
import com.example.FixItNow.dto.v1.SubServiceV1;
import com.example.FixItNow.service.CatalogV1Service;

import lombok.RequiredArgsConstructor;

/**
 * Public browse endpoints consumed by lib/booking.ts. Returns raw JSON arrays/objects
 * (not the ApiResponse envelope) because the new-API fetch client reads the body directly.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogV1Controller {

    private final CatalogV1Service catalogService;

    @GetMapping("/service-categories")
    public ResponseEntity<List<CategoryV1>> categories() {
        return ResponseEntity.ok(catalogService.listCategories());
    }

    @GetMapping("/service-categories/{categoryId}/sub-services")
    public ResponseEntity<List<SubServiceV1>> subServices(@PathVariable Long categoryId) {
        return ResponseEntity.ok(catalogService.listSubServices(categoryId));
    }

    @GetMapping("/sub-services/{id}")
    public ResponseEntity<SubServiceV1> subService(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getSubService(id));
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderV1>> providers(
            @RequestParam(name = "category_id", required = false) Long categoryId,
            @RequestParam(name = "available", required = false) Boolean available) {
        return ResponseEntity.ok(catalogService.listProviders(categoryId, available));
    }
}
