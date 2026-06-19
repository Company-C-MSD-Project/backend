package com.example.FixItNow.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.entity.WalletTransaction;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.UserRepository;
import com.example.FixItNow.repository.WalletTransactionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Provider wallet (services/wallet.ts). Earnings are derived from completed
 * bookings; withdrawals are persisted. Available balance = earnings − withdrawals;
 * pending = value held in escrow for active jobs.
 */
@Service
@RequiredArgsConstructor
public class WalletV1Service {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DOW = DateTimeFormatter.ofPattern("EEE");

    private final BookingRepository bookingRepository;
    private final WalletTransactionRepository walletTxnRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> balance(Long providerId) {
        requireUser(providerId);
        List<Booking> bookings = bookingRepository.findByProviderId(providerId);
        BigDecimal available = earnings(bookings).subtract(withdrawn(providerId));
        BigDecimal pending = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.ACCEPTED || b.getStatus() == BookingStatus.IN_PROGRESS)
                .map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("available", scale(available));
        m.put("pending", scale(pending));
        m.put("updated_at", LocalDateTime.now().toString());
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(Long providerId) {
        requireUser(providerId);
        List<Booking> completed = bookingRepository.findByProviderId(providerId).stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .toList();

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        BigDecimal thisWeek = sumBetween(completed, weekStart, today.plusDays(1));
        BigDecimal thisMonth = sumBetween(completed, today.withDayOfMonth(1), today.plusDays(1));
        BigDecimal ytd = sumBetween(completed, today.withDayOfYear(1), today.plusDays(1));
        BigDecimal total = completed.stream().map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgPerJob = completed.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(completed.size()), 2, RoundingMode.HALF_UP);

        // 7-day trend
        List<Map<String, Object>> trend = new ArrayList<>();
        BigDecimal trendSum = BigDecimal.ZERO;
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            BigDecimal v = sumBetween(completed, day, day.plusDays(1));
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("label", day.format(DOW));
            e.put("value", scale(v));
            e.put("active", i == 0);
            trend.add(e);
            trendSum = trendSum.add(v);
        }
        BigDecimal weekAvg = trendSum.divide(BigDecimal.valueOf(7), 2, RoundingMode.HALF_UP);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("this_week", scale(thisWeek));
        m.put("this_month", scale(thisMonth));
        m.put("ytd", scale(ytd));
        m.put("avg_per_job", scale(avgPerJob));
        m.put("weekly_trend", trend);
        m.put("week_avg", scale(weekAvg));
        m.put("growth_pct", 0);
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> transactions(Long providerId) {
        requireUser(providerId);
        List<Map<String, Object>> out = new ArrayList<>();

        // Credits derived from completed bookings.
        for (Booking b : bookingRepository.findByProviderId(providerId)) {
            if (b.getStatus() != BookingStatus.COMPLETED) continue;
            LocalDateTime when = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
            out.add(txn("BK" + String.format("%05d", b.getId()),
                    "Earnings — booking #" + b.getId(), when, "Job payment",
                    "+ " + money(amount(b)), "credit", when));
        }
        // Persisted withdrawals.
        for (WalletTransaction t : walletTxnRepository.findByUserIdOrderByCreatedAtDesc(providerId)) {
            // Payment credits are already represented by their completed booking above.
            if ("credit".equalsIgnoreCase(t.getTone())) continue;
            out.add(txn(t.getReference(), t.getDescription(), t.getCreatedAt(),
                    t.getType(), "- " + money(t.getAmount()), t.getTone(), t.getCreatedAt()));
        }
        out.sort(Comparator.comparing(m -> (LocalDateTime) m.get("_ts"), Comparator.reverseOrder()));
        out.forEach(m -> m.remove("_ts"));
        return out;
    }

    @Transactional
    public Map<String, Object> withdraw(Long providerId, BigDecimal amount, String accountId) {
        User user = requireUser(providerId);
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Withdrawal amount must be greater than 0.");
        }
        List<Booking> bookings = bookingRepository.findByProviderId(providerId);
        BigDecimal available = earnings(bookings).subtract(withdrawn(providerId));
        if (amount.compareTo(available) > 0) {
            throw new BadRequestException("Insufficient available balance.");
        }
        WalletTransaction txn = WalletTransaction.builder()
                .user(user)
                .description("Withdrawal to account " + (accountId != null ? accountId : "default"))
                .reference("WD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .type("Withdrawal")
                .tone("transfer")
                .amount(scale(amount))
                .build();
        walletTxnRepository.save(txn);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", true);
        m.put("available", scale(available.subtract(amount)));
        m.put("reference", txn.getReference());
        return m;
    }

    // ----- helpers -----

    private BigDecimal earnings(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal withdrawn(Long providerId) {
        return walletTxnRepository.findByUserIdOrderByCreatedAtDesc(providerId).stream()
                .filter(t -> !"credit".equalsIgnoreCase(t.getTone()))
                .map(WalletTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumBetween(List<Booking> bookings, LocalDate startInclusive, LocalDate endExclusive) {
        return bookings.stream()
                .filter(b -> {
                    LocalDateTime when = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
                    if (when == null) return false;
                    LocalDate d = when.toLocalDate();
                    return !d.isBefore(startInclusive) && d.isBefore(endExclusive);
                })
                .map(this::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> txn(String ref, String desc, LocalDateTime when, String type,
                                    String amount, String tone, LocalDateTime ts) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ref);
        m.put("desc", desc != null ? desc : "");
        m.put("date", when != null ? when.format(DATE_FMT) : "—");
        m.put("ref", ref);
        m.put("type", type != null ? type : "");
        m.put("amount", amount);
        m.put("tone", tone);
        m.put("_ts", ts != null ? ts : LocalDateTime.MIN);
        return m;
    }

    private BigDecimal amount(Booking b) {
        return b.getFinalCost() != null ? b.getFinalCost()
                : (b.getEstimatedCost() != null ? b.getEstimatedCost() : BigDecimal.ZERO);
    }

    private BigDecimal scale(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal v) {
        return "Rs " + scale(v).toPlainString();
    }

    private User requireUser(Long userId) {
        if (userId == null) throw new UnauthorizedException("Not authenticated.");
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Not authenticated."));
    }
}
