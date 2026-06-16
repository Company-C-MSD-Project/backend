package com.example.FixItNow.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.dto.v1.BookingV1;
import com.example.FixItNow.dto.v1.CreateBookingRequestV1;
import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.ServiceRepository;
import com.example.FixItNow.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Adapter for the /api/v1/bookings contract (lib/booking.ts). Maps the frontend's
 * Supabase-shaped payloads to the backend Booking entity, and delegates status
 * changes to the existing BookingService state machine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingV1Service {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final BookingService bookingService;
    private final NotificationService notificationService;

    @Transactional
    public BookingV1 create(CreateBookingRequestV1 req, Long currentUserId) {
        Long homeownerId = parseId(req.getHomeownerId(), "homeowner_id");
        if (homeownerId == null) homeownerId = currentUserId;
        if (homeownerId == null) throw new UnauthorizedException("Not authenticated.");

        final Long hoId = homeownerId;
        User homeowner = userRepository.findById(hoId)
                .orElseThrow(() -> new ResourceNotFoundException("Homeowner not found: " + hoId));
        if (homeowner.isBlacklisted()) {
            throw new BadRequestException("Your account has been suspended from making bookings.");
        }

        Long serviceId = parseId(req.getSubServiceId(), "sub_service_id");
        com.example.FixItNow.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));

        User provider = null;
        Long providerId = parseId(req.getProviderId(), "provider_id");
        if (providerId != null) {
            provider = userRepository.findById(providerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + providerId));
        }

        BigDecimal estimated = req.getTotalAmount() != null ? req.getTotalAmount() : service.getDayPayment();

        Booking booking = Booking.builder()
                .homeowner(homeowner)
                .provider(provider)
                .service(service)
                .serviceType(req.getServiceName())
                .bookingType(req.getJobType() != null ? req.getJobType() : "scheduled")
                .description(req.getProblemDesc())
                .serviceAddress(req.getAddressLine())
                .scheduledDate(resolveScheduledDate(req))
                .estimatedCost(estimated)
                .status(BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("v1 booking {} created by homeowner {} for service {}", saved.getId(), hoId, service.getName());

        if (provider != null) {
            notificationService.send(provider, "JOB_REQUEST",
                    "New job request: " + service.getName());
        }
        return toBookingV1(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingV1> listForProvider(Long providerId) {
        return bookingRepository.findByProviderId(providerId).stream()
                .map(this::toBookingV1)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingV1> listForHomeowner(Long homeownerId) {
        return bookingRepository.findByHomeownerId(homeownerId).stream()
                .map(this::toBookingV1)
                .toList();
    }

    /** PATCH /api/v1/bookings/{id}/status — maps the status string to the state machine. */
    @Transactional
    public BookingV1 updateStatus(Long bookingId, String status, String reason, Long currentUserId) {
        if (status == null) throw new BadRequestException("status is required.");
        Booking updated = switch (status.toLowerCase()) {
            case "accepted" -> {
                if (currentUserId == null) throw new UnauthorizedException("Not authenticated.");
                yield bookingService.acceptBooking(bookingId, currentUserId);
            }
            case "in_progress" -> bookingService.startJob(bookingId);
            case "completed" -> bookingService.completeJob(bookingId);
            case "cancelled" -> bookingService.cancelBooking(bookingId, reason);
            default -> throw new BadRequestException("Unsupported status transition: " + status);
        };
        return toBookingV1(updated);
    }

    // ----- helpers -----

    private LocalDateTime resolveScheduledDate(CreateBookingRequestV1 req) {
        // on_the_spot bookings (or missing date) are scheduled for now.
        if (req.getScheduledDate() == null || req.getScheduledDate().isBlank()) {
            return LocalDateTime.now();
        }
        try {
            LocalDate date = LocalDate.parse(req.getScheduledDate());
            LocalTime time = (req.getScheduledTime() != null && !req.getScheduledTime().isBlank())
                    ? LocalTime.parse(req.getScheduledTime())
                    : LocalTime.NOON;
            return LocalDateTime.of(date, time);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid scheduled_date/scheduled_time format.");
        }
    }

    private BookingV1 toBookingV1(Booking b) {
        User provider = b.getProvider();
        com.example.FixItNow.entity.Service service = b.getService();
        BigDecimal total = b.getFinalCost() != null ? b.getFinalCost() : b.getEstimatedCost();
        return BookingV1.builder()
                .id(String.valueOf(b.getId()))
                .refCode("FIN" + String.format("%05d", b.getId()))
                .homeownerId(b.getHomeowner() != null ? String.valueOf(b.getHomeowner().getId()) : null)
                .providerId(provider != null ? String.valueOf(provider.getId()) : null)
                .serviceName(service != null ? service.getName() : b.getServiceType())
                .jobType(b.getBookingType() != null ? b.getBookingType() : "scheduled")
                .scheduledDate(b.getScheduledDate() != null ? b.getScheduledDate().toLocalDate().toString() : null)
                .scheduledTime(b.getScheduledDate() != null ? b.getScheduledDate().toLocalTime().toString() : null)
                .addressLine(b.getServiceAddress())
                .district(null)
                .postalCode(null)
                .landmarks(null)
                .problemDesc(b.getDescription())
                .status(b.getStatus() != null ? b.getStatus().name().toLowerCase() : null)
                .hourlyRate(service != null ? service.getDayPayment() : null)
                .estHours(0)
                .platformFee(BigDecimal.ZERO)
                .totalAmount(total)
                .createdAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : null)
                .build();
    }

    private Long parseId(String raw, String field) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException(field + " must be a numeric id.");
        }
    }
}
