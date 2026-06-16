package com.example.FixItNow.service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.entity.ProviderRequest;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.repository.ProviderRequestRepository;

import lombok.RequiredArgsConstructor;

/**
 * Provider application workflow (services/provider-requests.ts). Persists the core
 * application fields; the rich presentation arrays (documents/checks/similar) are
 * returned empty for now. Approval verifies the linked provider User.
 */
@Service
@RequiredArgsConstructor
public class ProviderRequestV1Service {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final ProviderRequestRepository repository;
    private final UserService userService;

    /** Called at provider signup to open an application for admin review. */
    @Transactional
    public ProviderRequest createForProvider(User provider, String category) {
        ProviderRequest req = ProviderRequest.builder()
                .applicant(provider)
                .fullName(provider.getName())
                .displayName(provider.getName())
                .email(provider.getEmail())
                .phone(provider.getPhone())
                .category(category)
                .district(provider.getAddress())
                .status("Pending")
                .score(0)
                .scoreLabel("Awaiting review")
                .build();
        return repository.save(req);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        long pending = repository.countByStatus("Pending");
        long approved = repository.countByStatus("Approved");
        long rejected = repository.countByStatus("Rejected");
        m.put("pending", pending);
        m.put("approved", approved);
        m.put("rejected", rejected);
        m.put("total", pending + approved + rejected);
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        return toMap(require(id));
    }

    @Transactional
    public Map<String, Object> updateStatus(Long id, String status, String adminNotes) {
        ProviderRequest req = require(id);
        if (status != null) req.setStatus(normalize(status));
        if (adminNotes != null) req.setAdminNotes(adminNotes);
        repository.save(req);

        if ("Approved".equalsIgnoreCase(req.getStatus()) && req.getApplicant() != null) {
            userService.verifyProvider(req.getApplicant().getId());
        } else if ("Rejected".equalsIgnoreCase(req.getStatus()) && req.getApplicant() != null) {
            userService.setActiveStatus(req.getApplicant().getId(), false);
        }
        return toMap(req);
    }

    // ----- helpers -----

    private ProviderRequest require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Provider request not found: " + id));
    }

    private String normalize(String status) {
        return switch (status.toLowerCase()) {
            case "approved" -> "Approved";
            case "rejected" -> "Rejected";
            default -> "Pending";
        };
    }

    private Map<String, Object> toMap(ProviderRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(r.getId()));
        m.put("initials", initials(r.getDisplayName()));
        m.put("name", r.getDisplayName());
        m.put("fullName", r.getFullName());
        m.put("email", nz(r.getEmail()));
        m.put("phone", nz(r.getPhone()));
        m.put("nic", nz(r.getNic()));
        m.put("category", nz(r.getCategory()));
        m.put("categoryIcon", "🔧");
        m.put("subSpeciality", nz(r.getSubSpeciality()));
        m.put("district", nz(r.getDistrict()));
        m.put("experience", nz(r.getExperience()));
        m.put("hourlyRate", nz(r.getHourlyRate()));
        m.put("availability", nz(r.getAvailability()));
        m.put("applied", r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "—");
        m.put("appliedAt", r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "—");
        m.put("status", r.getStatus());
        m.put("score", r.getScore() != null ? r.getScore() : 0);
        m.put("scoreLabel", nz(r.getScoreLabel()));
        m.put("documents", List.of());
        m.put("adminNotes", nz(r.getAdminNotes()));
        m.put("checks", List.of());
        m.put("similar", List.of());
        return m;
    }

    private String nz(String s) {
        return s != null ? s : "";
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) {
            if (!parts[i].isEmpty()) sb.append(Character.toUpperCase(parts[i].charAt(0)));
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }
}
