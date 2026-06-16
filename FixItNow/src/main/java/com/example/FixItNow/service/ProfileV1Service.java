package com.example.FixItNow.service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.dto.v1.ProfileMeV1;
import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.DisputeStatus;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.DisputeRepository;
import com.example.FixItNow.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** Current-user profile + dashboard stats for the axios http client (services/profile.ts). */
@Service
@RequiredArgsConstructor
public class ProfileV1Service {

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy");

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final DisputeRepository disputeRepository;

    @Transactional(readOnly = true)
    public ProfileMeV1 me(Long userId) {
        return toProfile(requireUser(userId));
    }

    @Transactional
    public ProfileMeV1 update(Long userId, Map<String, Object> payload) {
        User user = requireUser(userId);
        String displayName = str(payload, "displayName", "display_name");
        if (displayName != null) user.setName(displayName);
        String phone = str(payload, "phone");
        if (phone != null) user.setPhone(phone);
        String address = str(payload, "address");
        if (address != null) user.setAddress(address);
        userRepository.save(user);
        return toProfile(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> homeownerStats(Long userId) {
        User user = requireUser(userId);
        List<Booking> bookings = bookingRepository.findByHomeownerId(userId);
        long active = bookings.stream().filter(b -> isActive(b.getStatus())).count();
        BigDecimal spent = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(b -> b.getFinalCost() != null ? b.getFinalCost() : safe(b.getEstimatedCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total_bookings", bookings.size());
        m.put("active_projects", active);
        m.put("total_spent", money(spent));
        m.put("wallet_balance", "—"); // wallet module not implemented yet
        m.put("reviews_given", 0);    // homeowner review query not implemented yet
        m.put("member_since", memberSince(user));
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminStats(Long userId) {
        User admin = requireUser(userId);
        long activeUsers = userRepository.findAll().stream().filter(User::isActive).count();
        long liveProviders = userRepository.findByUserType(UserType.SERVICE_PROVIDER).stream()
                .filter(User::isVerified).filter(User::isActive).count();
        long openTickets = disputeRepository.findByStatus(DisputeStatus.OPEN).size();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("active_users", String.valueOf(activeUsers));
        m.put("live_providers", String.valueOf(liveProviders));
        m.put("open_tickets", String.valueOf(openTickets));
        m.put("cluster_load", "—");
        m.put("last_audit", "—");
        m.put("member_since", memberSince(admin));
        return m;
    }

    // ----- helpers -----

    private ProfileMeV1 toProfile(User u) {
        return ProfileMeV1.builder()
                .id(String.valueOf(u.getId()))
                .email(u.getEmail())
                .username(u.getUsername())
                .displayName(u.getName())
                .avatarUrl(null)
                .role(uiRole(u.getUserType()))
                .phone(u.getPhone())
                .address(u.getAddress())
                .district(null)
                .bio(null)
                .build();
    }

    private User requireUser(Long userId) {
        if (userId == null) throw new UnauthorizedException("Not authenticated.");
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private boolean isActive(BookingStatus s) {
        return s == BookingStatus.PENDING || s == BookingStatus.ACCEPTED || s == BookingStatus.IN_PROGRESS;
    }

    private String memberSince(User u) {
        return u.getCreatedAt() != null ? u.getCreatedAt().format(MONTH_YEAR) : "—";
    }

    private String money(BigDecimal v) {
        return "Rs " + (v != null ? v.stripTrailingZeros().toPlainString() : "0");
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String uiRole(UserType type) {
        if (type == null) return "homeowner";
        return switch (type) {
            case SERVICE_PROVIDER -> "provider";
            case ADMIN -> "admin";
            case HOMEOWNER -> "homeowner";
        };
    }

    private String str(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }
}
