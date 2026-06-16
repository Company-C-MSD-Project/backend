package com.example.FixItNow.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

/**
 * Builds the UI-shaped active/past booking views for the homeowner dashboard
 * (services/bookings.ts) and provides the root /bookings/{id}/status façade.
 * Status changes delegate to the BookingService state machine.
 */
@Service
@RequiredArgsConstructor
public class BookingViewV1Service {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Transactional(readOnly = true)
    public Map<String, Object> active(Long homeownerId) {
        requireUser(homeownerId);
        List<Booking> all = bookingRepository.findByHomeownerId(homeownerId).stream()
                .filter(b -> isActive(b.getStatus()))
                .toList();

        long enRoute = all.stream().filter(b -> b.getStatus() == BookingStatus.ACCEPTED).count();
        long inProgress = all.stream().filter(b -> b.getStatus() == BookingStatus.IN_PROGRESS).count();
        BigDecimal escrow = all.stream()
                .map(b -> safe(b.getEstimatedCost())).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("active_now", all.size());
        stats.put("en_route", enRoute);
        stats.put("in_progress", inProgress);
        stats.put("in_escrow", money(escrow));

        List<Map<String, Object>> items = all.stream().map(this::activeItem).toList();
        return wrap(stats, items);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> past(Long homeownerId) {
        requireUser(homeownerId);
        List<Booking> all = bookingRepository.findByHomeownerId(homeownerId).stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED || b.getStatus() == BookingStatus.CANCELLED)
                .toList();

        long completed = all.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long cancelled = all.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        BigDecimal spent = all.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .map(b -> b.getFinalCost() != null ? b.getFinalCost() : safe(b.getEstimatedCost()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", all.size());
        stats.put("completed", completed);
        stats.put("cancelled", cancelled);
        stats.put("total_spent", money(spent));

        List<Map<String, Object>> items = all.stream().map(this::pastItem).toList();
        return wrap(stats, items);
    }

    /** Homeowner-facing status façade: cancel / confirm-done / reschedule. */
    @Transactional
    public void updateStatus(Long bookingId, Map<String, Object> body) {
        String status = str(body, "status");
        if (status == null) throw new BadRequestException("status is required.");
        switch (status.toLowerCase()) {
            case "cancelled" -> bookingService.cancelBooking(bookingId, str(body, "reason"));
            case "completed" -> bookingService.completeJob(bookingId);
            case "rescheduled" -> reschedule(bookingId, str(body, "date"), str(body, "time"));
            default -> throw new BadRequestException("Unsupported status: " + status);
        }
    }

    private void reschedule(Long bookingId, String date, String time) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BadRequestException("Booking not found: " + bookingId));
        if (date == null || date.isBlank()) throw new BadRequestException("date is required to reschedule.");
        try {
            LocalDate d = LocalDate.parse(date);
            LocalTime t = (time != null && !time.isBlank()) ? LocalTime.parse(time) : LocalTime.NOON;
            booking.setScheduledDate(LocalDateTime.of(d, t));
            bookingRepository.save(booking);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid date/time format for reschedule.");
        }
    }

    // ----- item mappers (presentation-shaped) -----

    private Map<String, Object> activeItem(Booking b) {
        Map<String, Object> m = baseItem(b);
        m.put("tone", toneFor(b.getStatus()));
        m.put("phase", phaseFor(b.getStatus()));
        m.put("eta", b.getEtaMinutes() != null ? b.getEtaMinutes() + " min" : "—");
        m.put("pay", "In escrow");
        m.put("actions", actionsForActive(b.getStatus()));
        return m;
    }

    private Map<String, Object> pastItem(Booking b) {
        Map<String, Object> m = baseItem(b);
        m.put("statusTone", b.getStatus() == BookingStatus.COMPLETED ? "ok" : "bad");
        m.put("pay", b.getStatus() == BookingStatus.COMPLETED ? "Paid" : "Refunded");
        m.put("actions", b.getStatus() == BookingStatus.COMPLETED
                ? List.of("Rebook", "Review") : List.of("Rebook"));
        m.put("strike", b.getStatus() == BookingStatus.CANCELLED);
        return m;
    }

    private Map<String, Object> baseItem(Booking b) {
        User provider = b.getProvider();
        com.example.FixItNow.entity.Service service = b.getService();
        String providerName = provider != null ? provider.getName() : "Unassigned";
        String serviceName = service != null ? service.getName() : (b.getServiceType() != null ? b.getServiceType() : "Service");
        BigDecimal total = b.getFinalCost() != null ? b.getFinalCost() : safe(b.getEstimatedCost());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(b.getId()));
        m.put("ref", "FIN" + String.format("%05d", b.getId()));
        m.put("icon", "🛠️");
        m.put("iconBg", "oklch(0.55 0.10 60)");
        m.put("title", serviceName);
        m.put("status", b.getStatus() != null ? b.getStatus().name().toLowerCase() : "");
        m.put("cat", service != null && service.getCategory() != null ? service.getCategory().getCategoryType() : "General");
        m.put("date", b.getScheduledDate() != null ? b.getScheduledDate().toLocalDate().format(DATE_FMT) : "—");
        m.put("time", b.getScheduledDate() != null ? b.getScheduledDate().toLocalTime().format(TIME_FMT) : "—");
        m.put("addr", b.getServiceAddress() != null ? b.getServiceAddress() : "—");
        m.put("provider", providerName);
        m.put("pInit", initials(providerName));
        m.put("price", money(total));
        return m;
    }

    // ----- helpers -----

    private Map<String, Object> wrap(Map<String, Object> stats, List<Map<String, Object>> items) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stats", stats);
        out.put("items", items);
        return out;
    }

    private boolean isActive(BookingStatus s) {
        return s == BookingStatus.PENDING || s == BookingStatus.ACCEPTED || s == BookingStatus.IN_PROGRESS;
    }

    private String toneFor(BookingStatus s) {
        return switch (s) {
            case PENDING -> "info";
            case ACCEPTED -> "ok";
            case IN_PROGRESS -> "warn";
            default -> "info";
        };
    }

    private String phaseFor(BookingStatus s) {
        return switch (s) {
            case PENDING -> "Awaiting provider";
            case ACCEPTED -> "Provider en route";
            case IN_PROGRESS -> "Work in progress";
            default -> "—";
        };
    }

    private List<String> actionsForActive(BookingStatus s) {
        return switch (s) {
            case PENDING -> List.of("Reschedule", "Cancel");
            case ACCEPTED -> List.of("Track", "Cancel");
            case IN_PROGRESS -> List.of("Track", "Confirm done");
            default -> List.of();
        };
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

    private String money(BigDecimal v) {
        return "Rs " + (v != null ? v.stripTrailingZeros().toPlainString() : "0");
    }

    private BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private void requireUser(Long userId) {
        if (userId == null) throw new UnauthorizedException("Not authenticated.");
    }

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? String.valueOf(v) : null;
    }
}
