package com.example.FixItNow.service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.entity.Category;
import com.example.FixItNow.entity.CategoryRequest;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.repository.CategoryRepository;
import com.example.FixItNow.repository.CategoryRequestRepository;

import lombok.RequiredArgsConstructor;

/**
 * New-category request workflow (services/category-requests.ts, services/admin.ts).
 * Persists the core request fields; on approval (status "Active") a matching catalog
 * Category is created if one does not already exist.
 */
@Service
@RequiredArgsConstructor
public class CategoryRequestV1Service {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final CategoryRequestRepository repository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        long pending = repository.countByStatus("Pending");
        long active = repository.countByStatus("Active");
        long rejected = repository.countByStatus("Rejected");
        m.put("pending", pending);
        m.put("active", active);
        m.put("rejected", rejected);
        m.put("total", pending + active + rejected);
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        return toMap(require(id));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> payload) {
        String name = str(payload, "name");
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Category name is required.");
        }
        CategoryRequest req = CategoryRequest.builder()
                .name(name)
                .icon(str(payload, "icon"))
                .subtitle(str(payload, "subtitle"))
                .requestedBy(str(payload, "requestedBy", "requested_by"))
                .requesterEmail(str(payload, "requesterEmail", "requester_email"))
                .contact(str(payload, "contact"))
                .priceRange(str(payload, "priceRange", "price_range"))
                .platformFee(str(payload, "platformFee", "platform_fee"))
                .demand(str(payload, "demand"))
                .description(str(payload, "description"))
                .status("Pending")
                .build();
        return toMap(repository.save(req));
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, String status, String adminNotes) {
        CategoryRequest req = require(id);
        if (status != null) req.setStatus(normalize(status));
        if (adminNotes != null) req.setAdminNotes(adminNotes);
        repository.save(req);

        // On approval, promote into the live catalog if not already present.
        if ("Active".equalsIgnoreCase(req.getStatus()) && !categoryRepository.existsByCategoryType(req.getName())) {
            categoryRepository.save(Category.builder()
                    .categoryType(req.getName())
                    .description(req.getDescription())
                    .isActive(true)
                    .build());
        }
        return toMap(req);
    }

    // ----- helpers -----

    private CategoryRequest require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category request not found: " + id));
    }

    private String normalize(String status) {
        return switch (status.toLowerCase()) {
            case "active", "approved" -> "Active";
            case "rejected" -> "Rejected";
            default -> "Pending";
        };
    }

    private Map<String, Object> toMap(CategoryRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(r.getId()));
        m.put("icon", r.getIcon() != null ? r.getIcon() : "🛠️");
        m.put("name", r.getName());
        m.put("subtitle", nz(r.getSubtitle()));
        m.put("requestedBy", nz(r.getRequestedBy()));
        m.put("requesterEmail", nz(r.getRequesterEmail()));
        m.put("contact", nz(r.getContact()));
        m.put("providersWaiting", r.getProvidersWaiting() != null ? r.getProvidersWaiting() : 0);
        m.put("applied", r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "—");
        m.put("status", r.getStatus());
        m.put("priceRange", nz(r.getPriceRange()));
        m.put("platformFee", nz(r.getPlatformFee()));
        m.put("demand", nz(r.getDemand()));
        m.put("description", nz(r.getDescription()));
        m.put("subCategories", List.of());
        m.put("adminNotes", nz(r.getAdminNotes()));
        m.put("requestedAgo", r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "—");
        m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "—");
        m.put("monthlyBookings", "—");
        m.put("monthlyRevenue", "—");
        m.put("searchHits", "—");
        m.put("providers", List.of());
        m.put("extraProviders", 0);
        return m;
    }

    private String nz(String s) {
        return s != null ? s : "";
    }

    private String str(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }
}
