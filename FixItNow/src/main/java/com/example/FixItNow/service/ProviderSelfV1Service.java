package com.example.FixItNow.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

/** Provider dashboard stats for /providers/me/stats (services/provider-stats.ts). */
@Service
@RequiredArgsConstructor
public class ProviderSelfV1Service {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM");

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> myStats(Long providerId) {
        if (providerId == null) throw new UnauthorizedException("Not authenticated.");

        List<Booking> bookings = bookingRepository.findByProviderId(providerId);
        long activeJobs = bookings.stream().filter(b -> b.getStatus() == BookingStatus.ACCEPTED
                || b.getStatus() == BookingStatus.IN_PROGRESS).count();
        long completed = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        BigDecimal weekly = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .filter(b -> b.getUpdatedAt() != null && b.getUpdatedAt().isAfter(weekAgo))
                .map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgRating = reviewRepository.findAverageRatingByProviderId(providerId).orElse(0.0);
        int completionRate = bookings.isEmpty() ? 0 : (int) Math.round(100.0 * completed / bookings.size());

        // Monthly earnings for the last 6 calendar months.
        List<Map<String, Object>> monthly = new ArrayList<>();
        BigDecimal peak = BigDecimal.ZERO;
        BigDecimal ytd = BigDecimal.ZERO;
        int currentYear = LocalDate.now().getYear();
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = LocalDate.now().withDayOfMonth(1).minusMonths(i);
            LocalDate monthEnd = monthStart.plusMonths(1);
            BigDecimal sum = bookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getUpdatedAt() != null)
                    .filter(b -> {
                        LocalDate d = b.getUpdatedAt().toLocalDate();
                        return !d.isBefore(monthStart) && d.isBefore(monthEnd);
                    })
                    .map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", monthStart.format(MONTH));
            entry.put("amount", sum);
            monthly.add(entry);
            if (sum.compareTo(peak) > 0) peak = sum;
            if (monthStart.getYear() == currentYear) ytd = ytd.add(sum);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("active_jobs", hinted(activeJobs, "in progress"));
        m.put("weekly_earnings", hinted(money(weekly), "last 7 days"));
        m.put("avg_rating", hinted(round1(avgRating), "from reviews"));
        m.put("completion_rate", hinted(completionRate + "%", "of all jobs"));
        m.put("monthly", monthly);
        m.put("monthly_peak", money(peak));
        m.put("monthly_ytd", money(ytd));
        return m;
    }

    private Map<String, Object> hinted(Object value, String hint) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("hint", hint);
        return m;
    }

    private BigDecimal amount(Booking b) {
        return b.getFinalCost() != null ? b.getFinalCost()
                : (b.getEstimatedCost() != null ? b.getEstimatedCost() : BigDecimal.ZERO);
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String money(BigDecimal v) {
        return "Rs " + (v != null ? v.stripTrailingZeros().toPlainString() : "0");
    }
}
