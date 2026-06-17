package com.example.FixItNow.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.dto.v1.ProviderServiceCardV1;
import com.example.FixItNow.entity.ProviderServiceCard;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.exception.ForbiddenException;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.ProviderServiceRepository;
import com.example.FixItNow.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Provider-owned "Service Cards" CRUD (ProviderServiceCardsPage / services/services.ts).
 * Distinct from the admin-curated /api/services catalog ({@link ServiceManagementService}) —
 * each card here belongs to exactly one SERVICE_PROVIDER, enforced on every read/write.
 */
@Service
@RequiredArgsConstructor
public class ProviderServiceV1Service {

    private final ProviderServiceRepository repository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProviderServiceCardV1> list(Long providerId) {
        requireUser(providerId);
        return repository.findByProviderIdOrderByCreatedAtDesc(providerId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ProviderServiceCardV1 create(Long providerId, Map<String, Object> payload) {
        User provider = requireUser(providerId);
        String title = str(payload, "title");
        if (title == null || title.isBlank()) throw new BadRequestException("Title is required.");

        ProviderServiceCard card = ProviderServiceCard.builder()
                .provider(provider)
                .title(title)
                .category(str(payload, "category"))
                .rateType(str(payload, "rate_type", "rateType"))
                .rateAmount(decimal(payload, "rate_amount", "rateAmount"))
                .minFee(decimal(payload, "min_fee", "minFee"))
                .shortSummary(str(payload, "short_summary", "shortSummary"))
                .fullDescription(str(payload, "full_description", "fullDescription"))
                .duration(str(payload, "duration"))
                .warranty(str(payload, "warranty"))
                .coverImage(str(payload, "cover_image", "coverImage"))
                .status(orDefault(str(payload, "status"), "draft"))
                .districts(toCsv(payload.get("districts")))
                .emoji(str(payload, "emoji"))
                .bg(str(payload, "bg"))
                .priceLine(str(payload, "price_line", "priceLine"))
                .published(bool(payload, "published"))
                .build();
        return toDto(repository.save(card));
    }

    @Transactional
    public ProviderServiceCardV1 update(Long providerId, Long id, Map<String, Object> payload) {
        ProviderServiceCard card = requireOwned(providerId, id);

        if (payload.containsKey("title")) card.setTitle(str(payload, "title"));
        if (payload.containsKey("category")) card.setCategory(str(payload, "category"));
        if (has(payload, "rate_type", "rateType")) card.setRateType(str(payload, "rate_type", "rateType"));
        if (has(payload, "rate_amount", "rateAmount")) card.setRateAmount(decimal(payload, "rate_amount", "rateAmount"));
        if (has(payload, "min_fee", "minFee")) card.setMinFee(decimal(payload, "min_fee", "minFee"));
        if (has(payload, "short_summary", "shortSummary")) card.setShortSummary(str(payload, "short_summary", "shortSummary"));
        if (has(payload, "full_description", "fullDescription")) card.setFullDescription(str(payload, "full_description", "fullDescription"));
        if (payload.containsKey("duration")) card.setDuration(str(payload, "duration"));
        if (payload.containsKey("warranty")) card.setWarranty(str(payload, "warranty"));
        if (has(payload, "cover_image", "coverImage")) card.setCoverImage(str(payload, "cover_image", "coverImage"));
        if (payload.containsKey("status")) card.setStatus(str(payload, "status"));
        if (payload.containsKey("districts")) card.setDistricts(toCsv(payload.get("districts")));
        if (payload.containsKey("emoji")) card.setEmoji(str(payload, "emoji"));
        if (payload.containsKey("bg")) card.setBg(str(payload, "bg"));
        if (has(payload, "price_line", "priceLine")) card.setPriceLine(str(payload, "price_line", "priceLine"));
        // togglePublish sends {published, status} together — both handled above/below.
        if (payload.containsKey("published")) card.setPublished(bool(payload, "published"));

        return toDto(repository.save(card));
    }

    @Transactional
    public void delete(Long providerId, Long id) {
        repository.delete(requireOwned(providerId, id));
    }

    // ----- helpers -----

    /** Loads the card and verifies it belongs to the requesting provider — 404 if missing, 403 if not theirs. */
    private ProviderServiceCard requireOwned(Long providerId, Long id) {
        requireUser(providerId);
        ProviderServiceCard card = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service card not found: " + id));
        if (card.getProvider() == null || !providerId.equals(card.getProvider().getId())) {
            throw new ForbiddenException("You do not own this service card.");
        }
        return card;
    }

    private User requireUser(Long providerId) {
        if (providerId == null) throw new UnauthorizedException("Not authenticated.");
        return userRepository.findById(providerId)
                .orElseThrow(() -> new UnauthorizedException("Not authenticated."));
    }

    private ProviderServiceCardV1 toDto(ProviderServiceCard c) {
        return ProviderServiceCardV1.builder()
                .id(String.valueOf(c.getId()))
                .title(c.getTitle())
                .category(c.getCategory())
                .rateType(c.getRateType())
                .rateAmount(c.getRateAmount())
                .minFee(c.getMinFee())
                .shortSummary(c.getShortSummary())
                .fullDescription(c.getFullDescription())
                .duration(c.getDuration())
                .warranty(c.getWarranty())
                .coverImage(c.getCoverImage())
                .status(c.getStatus())
                .districts(fromCsv(c.getDistricts()))
                .emoji(c.getEmoji())
                .bg(c.getBg())
                .priceLine(c.getPriceLine())
                .published(c.isPublished())
                .build();
    }

    private boolean has(Map<String, Object> m, String... keys) {
        for (String k : keys) if (m.containsKey(k)) return true;
        return false;
    }

    /** Accepts a JSON array (frontend's actual shape) or a plain string; stores as CSV. */
    private String toCsv(Object raw) {
        if (raw == null) return null;
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::trim)
                    .filter(s -> !s.isEmpty()).reduce((a, b) -> a + "," + b).orElse(null);
        }
        return String.valueOf(raw);
    }

    private List<String> fromCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private BigDecimal decimal(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) {
                if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
                try {
                    return new BigDecimal(String.valueOf(v));
                } catch (NumberFormatException ex) {
                    throw new BadRequestException("Invalid number for " + k + ": " + v);
                }
            }
        }
        return null;
    }

    private boolean bool(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v));
    }

    private String orDefault(String v, String def) {
        return v != null && !v.isBlank() ? v : def;
    }

    private String str(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }
}
