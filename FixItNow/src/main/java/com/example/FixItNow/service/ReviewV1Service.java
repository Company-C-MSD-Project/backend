package com.example.FixItNow.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.dto.v1.ProviderReviewV1;
import com.example.FixItNow.dto.v1.ReviewV1;
import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.Review;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.ReviewRepository;
import com.example.FixItNow.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Review moderation (admin) + provider self-service reviews/summary/reply. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewV1Service {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    // ===== Admin moderation =====

    @Transactional(readOnly = true)
    public List<ReviewV1> listAll() {
        return reviewRepository.findAll().stream().map(this::toReviewV1).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        List<Review> reviews = reviewRepository.findAll();
        long total = reviews.size();
        double avg = reviews.stream().filter(r -> r.getRating() != null)
                .mapToInt(Review::getRating).average().orElse(0.0);
        long positive = reviews.stream().filter(r -> r.getRating() != null && r.getRating() >= 4).count();
        long flagged = reviews.stream().filter(Review::isFlagged).count();

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int stars = 5; stars >= 1; stars--) {
            final int s = stars;
            long count = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == s).count();
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("stars", s);
            d.put("count", count);
            d.put("total", total);
            distribution.add(d);
        }

        List<Map<String, Object>> topProviders = userRepository.findByUserType(UserType.SERVICE_PROVIDER).stream()
                .filter(p -> p.getRating() != null && p.getRating() > 0)
                .sorted(Comparator.comparing(User::getRating).reversed())
                .limit(3)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", String.valueOf(p.getId()));
                    m.put("initials", initials(p.getName()));
                    m.put("name", p.getName());
                    m.put("meta", p.getServiceCategory() != null ? p.getServiceCategory() : "Provider");
                    m.put("rating", round1(p.getRating()));
                    return m;
                })
                .toList();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("avg", round1(avg));
        m.put("total", total);
        m.put("positive", positive);
        m.put("flagged", flagged);
        m.put("awaiting_reply", 0); // reply tracking not implemented yet
        m.put("distribution", distribution);
        m.put("top_providers", topProviders);
        return m;
    }

    @Transactional
    public void flag(Long id) {
        setFlag(id, true);
    }

    @Transactional
    public void hide(Long id) {
        // No dedicated "hidden" column yet; treat as flagged for moderation.
        setFlag(id, true);
    }

    @Transactional
    public void resolve(Long id) {
        setFlag(id, false);
    }

    @Transactional
    public Map<String, Object> reply(Long id, String message) {
        // No reply entity yet — acknowledge so the UI flow completes (persistence is a later step).
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        log.info("Provider reply to review {}: {}", review.getId(), message);
        return Map.of("ok", true);
    }

    // ===== Provider self-service =====

    @Transactional(readOnly = true)
    public List<ProviderReviewV1> myReviews(Long providerId) {
        requireUser(providerId);
        return reviewRepository.findByProviderId(providerId).stream()
                .map(this::toProviderReview)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> mySummary(Long providerId) {
        requireUser(providerId);
        List<Review> reviews = reviewRepository.findByProviderId(providerId);
        List<Booking> bookings = bookingRepository.findByProviderId(providerId);
        long completed = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        double avg = reviews.stream().filter(r -> r.getRating() != null)
                .mapToInt(Review::getRating).average().orElse(0.0);
        BigDecimal earned = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(b -> b.getFinalCost() != null ? b.getFinalCost() : safe(b.getEstimatedCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int completionRate = bookings.isEmpty() ? 0 : (int) Math.round(100.0 * completed / bookings.size());

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (int stars = 5; stars >= 1; stars--) {
            final int s = stars;
            long count = reviews.stream().filter(r -> r.getRating() != null && r.getRating() == s).count();
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("stars", s);
            d.put("count", count);
            d.put("total", (long) reviews.size());
            distribution.add(d);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("avg_rating", round1(avg));
        m.put("total_reviews", reviews.size());
        m.put("completion_rate", completionRate);
        m.put("total_earned", money(earned));
        m.put("jobs_done", completed);
        m.put("experience", "—");
        m.put("distribution", distribution);
        return m;
    }

    // ===== mappers / helpers =====

    private ReviewV1 toReviewV1(Review r) {
        Booking booking = r.getBooking();
        User homeowner = r.getHomeowner();
        User provider = r.getProvider();
        String serviceName = booking != null && booking.getService() != null ? booking.getService().getName() : "—";
        BigDecimal amount = booking != null
                ? (booking.getFinalCost() != null ? booking.getFinalCost() : safe(booking.getEstimatedCost()))
                : BigDecimal.ZERO;
        return ReviewV1.builder()
                .id(String.valueOf(r.getId()))
                .initials(initials(homeowner != null ? homeowner.getName() : null))
                .name(homeowner != null ? homeowner.getName() : "—")
                .city("—")
                .date(r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "—")
                .text(r.getComment() != null ? r.getComment() : "")
                .service(serviceName)
                .pro(provider != null ? provider.getName() : "—")
                .amount(money(amount))
                .rating(r.getRating() != null ? r.getRating() : 0)
                .flagged(r.isFlagged())
                .build();
    }

    private ProviderReviewV1 toProviderReview(Review r) {
        Booking booking = r.getBooking();
        User homeowner = r.getHomeowner();
        String serviceName = booking != null && booking.getService() != null ? booking.getService().getName() : "—";
        BigDecimal amount = booking != null
                ? (booking.getFinalCost() != null ? booking.getFinalCost() : safe(booking.getEstimatedCost()))
                : BigDecimal.ZERO;
        return ProviderReviewV1.builder()
                .id(String.valueOf(r.getId()))
                .name(homeowner != null ? homeowner.getName() : "—")
                .city("—")
                .service(serviceName)
                .date(r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "—")
                .paid(money(amount))
                .rating(r.getRating() != null ? r.getRating() : 0)
                .text(r.getComment() != null ? r.getComment() : "")
                .build();
    }

    private void setFlag(Long id, boolean flagged) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
        review.setFlagged(flagged);
        reviewRepository.save(review);
    }

    private void requireUser(Long userId) {
        if (userId == null) throw new UnauthorizedException("Not authenticated.");
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String money(BigDecimal v) {
        return "Rs " + (v != null ? v.stripTrailingZeros().toPlainString() : "0");
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
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
