package com.example.FixItNow.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.dto.v1.HomeownerAdminV1;
import com.example.FixItNow.dto.v1.ProviderAdminV1;
import com.example.FixItNow.dto.v1.ProviderRegistrationV1;
import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.BadgeLevel;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.DisputeStatus;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.DisputeRepository;
import com.example.FixItNow.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** Admin views and actions over existing users/bookings (providers, homeowners, dashboard). */
@Service
@RequiredArgsConstructor
public class AdminV1Service {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy");

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final DisputeRepository disputeRepository;
    private final UserService userService;

    // ===== Providers (admin) =====

    @Transactional(readOnly = true)
    public List<ProviderAdminV1> listProviders() {
        return userRepository.findByUserType(UserType.SERVICE_PROVIDER).stream()
                .map(this::toProviderAdmin)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> providerStats() {
        List<User> providers = userRepository.findByUserType(UserType.SERVICE_PROVIDER);
        long active = providers.stream().filter(User::isActive).filter(User::isVerified).count();
        long categories = providers.stream()
                .map(User::getServiceCategory).filter(c -> c != null && !c.isBlank()).distinct().count();
        double avgRating = providers.stream().filter(p -> p.getRating() != null)
                .mapToDouble(User::getRating).average().orElse(0.0);
        long suspended = providers.stream().filter(p -> !p.isActive()).count();
        long topRated = providers.stream().filter(p -> p.getBadgeLevel() == BadgeLevel.TOP_RATED).count();
        long newCount = providers.stream().filter(this::isNew).count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("active", active);
        m.put("categories", categories);
        m.put("avg_rating", round1(avgRating));
        m.put("suspended", suspended);
        m.put("top_rated", topRated);
        m.put("new_count", newCount);
        return m;
    }

    @Transactional(readOnly = true)
    public List<ProviderRegistrationV1> pendingProviders() {
        return userRepository.findByUserType(UserType.SERVICE_PROVIDER).stream()
                .filter(p -> !p.isVerified())
                .map(p -> ProviderRegistrationV1.builder()
                        .id(String.valueOf(p.getId()))
                        .initials(initials(p.getName()))
                        .name(p.getName())
                        .category(p.getServiceCategory() != null ? p.getServiceCategory() : "—")
                        .location(p.getAddress() != null ? p.getAddress() : "—")
                        .submittedAt(p.getCreatedAt() != null ? p.getCreatedAt().format(DATE_FMT) : "—")
                        .build())
                .toList();
    }

    @Transactional
    public void approveProvider(Long id) {
        userService.verifyProvider(id);
    }

    @Transactional
    public void rejectProvider(Long id) {
        userService.setActiveStatus(id, false);
    }

    @Transactional
    public void suspendProvider(Long id) {
        userService.setActiveStatus(id, false);
    }

    @Transactional
    public void reinstateProvider(Long id) {
        userService.setActiveStatus(id, true);
    }

    // ===== Homeowners (admin) =====

    @Transactional(readOnly = true)
    public List<HomeownerAdminV1> listHomeowners() {
        return userRepository.findByUserType(UserType.HOMEOWNER).stream()
                .map(this::toHomeownerAdmin)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> homeownerStats() {
        List<User> homeowners = userRepository.findByUserType(UserType.HOMEOWNER);
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        long active = homeowners.stream().filter(User::isActive).filter(h -> !h.isBlacklisted()).count();
        long joined = homeowners.stream()
                .filter(h -> h.getCreatedAt() != null && h.getCreatedAt().isAfter(weekAgo)).count();
        long flagged = homeowners.stream().filter(User::isBlacklisted).count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", homeowners.size());
        m.put("active", active);
        m.put("joined_this_week", joined);
        m.put("flagged", flagged);
        return m;
    }

    @Transactional
    public void suspendHomeowner(Long id) {
        userService.setActiveStatus(id, false);
    }

    // ===== Dashboard =====

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<User> homeowners = userRepository.findByUserType(UserType.HOMEOWNER);
        List<User> providers = userRepository.findByUserType(UserType.SERVICE_PROVIDER);
        long pendingProviders = providers.stream().filter(p -> !p.isVerified()).count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("homeowners", Map.of("total", homeowners.size(), "delta_week", countSince(homeowners, weekAgo)));
        m.put("providers", Map.of("total", providers.size(), "delta_week", countSince(providers, weekAgo)));
        m.put("pending", Map.of("total", pendingProviders, "provider", pendingProviders, "category", 0));
        m.put("revenue_today", Map.of("value", "Rs 0", "delta_pct", 0));
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> metrics() {
        List<Booking> bookings = bookingRepository.findAll();
        long active = bookings.stream().filter(b -> b.getStatus() == BookingStatus.ACCEPTED
                || b.getStatus() == BookingStatus.IN_PROGRESS).count();
        long completed = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long verified = userRepository.findByUserType(UserType.SERVICE_PROVIDER).stream()
                .filter(User::isVerified).count();
        long openDisputes = disputeRepository.findByStatus(DisputeStatus.OPEN).size();

        return List.of(
                metric("Active Bookings", String.valueOf(active), pct(active, bookings.size()), "primary"),
                metric("Completed Jobs", String.valueOf(completed), pct(completed, bookings.size()), "success"),
                metric("Verified Providers", String.valueOf(verified), 100, "success"),
                metric("Open Disputes", String.valueOf(openDisputes), pct(openDisputes, bookings.size()), "primary"));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> activity() {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getCreatedAt() != null)
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .limit(8)
                .map(b -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", String.valueOf(b.getId()));
                    m.put("type", activityType(b.getStatus()));
                    m.put("message", "Booking #" + b.getId() + " — " + b.getStatus().name().toLowerCase());
                    m.put("time", b.getCreatedAt().format(DATE_FMT));
                    return m;
                })
                .toList();
    }

    // ===== mappers / helpers =====

    private ProviderAdminV1 toProviderAdmin(User p) {
        long jobs = bookingRepository.countByProviderIdAndStatus(p.getId(), BookingStatus.COMPLETED);
        return ProviderAdminV1.builder()
                .id(String.valueOf(p.getId()))
                .initials(initials(p.getName()))
                .name(p.getName())
                .email(p.getEmail())
                .category(p.getServiceCategory() != null ? p.getServiceCategory() : "—")
                .icon("🛠️")
                .district(p.getAddress() != null ? p.getAddress() : "—")
                .jobs(jobs)
                .rating(p.getRating() != null ? round1(p.getRating()) : 0.0)
                .status(providerStatus(p, jobs))
                .build();
    }

    private HomeownerAdminV1 toHomeownerAdmin(User h) {
        List<Booking> bookings = bookingRepository.findByHomeownerId(h.getId());
        BigDecimal spent = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(b -> b.getFinalCost() != null ? b.getFinalCost() : safe(b.getEstimatedCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return HomeownerAdminV1.builder()
                .id(String.valueOf(h.getId()))
                .initials(initials(h.getName()))
                .name(h.getName())
                .email(h.getEmail())
                .location(h.getAddress() != null ? h.getAddress() : "—")
                .bookings(bookings.size())
                .spent(money(spent))
                .memberSince(h.getCreatedAt() != null ? h.getCreatedAt().format(MONTH_YEAR) : "—")
                .status(h.isBlacklisted() ? "Flagged" : (bookings.isEmpty() ? "New" : "Active"))
                .build();
    }

    private String providerStatus(User p, long jobs) {
        if (!p.isActive()) return "Suspended";
        if (p.getBadgeLevel() == BadgeLevel.TOP_RATED) return "Top";
        if (isNew(p) && jobs == 0) return "New";
        return "Active";
    }

    private boolean isNew(User p) {
        return p.getCreatedAt() != null && p.getCreatedAt().isAfter(LocalDateTime.now().minusDays(14));
    }

    private long countSince(List<User> users, LocalDateTime since) {
        return users.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(since)).count();
    }

    private Map<String, Object> metric(String label, String value, int pct, String tone) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        m.put("pct", pct);
        m.put("tone", tone);
        return m;
    }

    private String activityType(BookingStatus s) {
        return switch (s) {
            case COMPLETED -> "success";
            case CANCELLED -> "destructive";
            case PENDING -> "info";
            default -> "primary";
        };
    }

    private int pct(long part, long total) {
        if (total <= 0) return 0;
        return (int) Math.min(100, Math.round(100.0 * part / total));
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
