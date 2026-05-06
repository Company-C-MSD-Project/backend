package com.example.FixItNow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Booking lifecycle management (SRS FR8).
 * Enforces the state-transition rules from the SRS State-Transition Diagram:
 *   PENDING → ACCEPTED (provider confirms)
 *   ACCEPTED → IN_PROGRESS (provider arrives)
 *   IN_PROGRESS → COMPLETED (job done)
 *   PENDING | ACCEPTED → CANCELLED (either party)
 * Invalid transitions throw BadRequestException — never silently ignored.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final NotificationService notificationService;

    @Transactional
    public Booking createBooking(Long homeownerId, BookingRequest req) {
        User homeowner = userRepository.findById(homeownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Homeowner not found"));
        if (homeowner.isBlacklisted()) {
            throw new BadRequestException("Your account has been suspended from making bookings.");
        }

        Service service = serviceRepository.findById(req.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + req.getServiceId()));

        User provider = null;
        if (req.getProviderId() != null) {
            provider = userRepository.findById(req.getProviderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));
            if (!provider.isVerified()) {
                throw new BadRequestException("Selected provider is not yet verified.");
            }
        }

        // Estimate cost = service day rate (actual cost confirmed on job completion)
        BigDecimal estimatedCost = service.getDayPayment();

        Booking booking = Booking.builder()
                .homeowner(homeowner)
                .provider(provider)
                .service(service)
                .serviceType(req.getServiceType())
                .description(req.getDescription())
                .scheduledDate(req.getScheduledDate())
                .estimatedCost(estimatedCost)
                .status(BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} created by homeowner {} for service {}", saved.getId(), homeownerId, service.getName());

        // Notify provider (if assigned) about new job request
        if (provider != null) {
            notificationService.send(provider, "JOB_REQUEST",
                    "New job request: " + service.getName() + " on " + req.getScheduledDate());
        }

        return saved;
    }

    /** Provider accepts a pending booking — transitions PENDING → ACCEPTED. */
    @Transactional
    public Booking acceptBooking(Long bookingId, Long providerId) {
        Booking booking = getBookingOrThrow(bookingId);
        assertStatus(booking, BookingStatus.PENDING);

        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        booking.setProvider(provider);
        booking.setStatus(BookingStatus.ACCEPTED);
        Booking saved = bookingRepository.save(booking);

        notificationService.send(booking.getHomeowner(), "BOOKING_ACCEPTED",
                "Your booking #" + bookingId + " has been accepted by " + provider.getName() + ".");
        return saved;
    }

    /** Provider marks arrival — transitions ACCEPTED → IN_PROGRESS. */
    @Transactional
    public Booking startJob(Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        assertStatus(booking, BookingStatus.ACCEPTED);
        booking.setStatus(BookingStatus.IN_PROGRESS);
        Booking saved = bookingRepository.save(booking);

        notificationService.send(booking.getHomeowner(), "JOB_STARTED",
                "Your service provider has arrived and started working on booking #" + bookingId);
        return saved;
    }

    /** Provider marks job done — transitions IN_PROGRESS → COMPLETED. */
    @Transactional
    public Booking completeJob(Long bookingId) {
        Booking booking = getBookingOrThrow(bookingId);
        assertStatus(booking, BookingStatus.IN_PROGRESS);
        booking.setStatus(BookingStatus.COMPLETED);
        Booking saved = bookingRepository.save(booking);

        notificationService.send(booking.getHomeowner(), "JOB_COMPLETED",
                "Booking #" + bookingId + " is complete. Please make payment and leave a review.");
        return saved;
    }

    /** Either party cancels — only allowed in PENDING or ACCEPTED state. */
    @Transactional
    public Booking cancelBooking(Long bookingId, String reason) {
        Booking booking = getBookingOrThrow(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new BadRequestException("Cannot cancel a booking in status: " + booking.getStatus());
        }
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        return bookingRepository.save(booking);
    }

    public List<Booking> getByHomeowner(Long homeownerId) {
        return bookingRepository.findByHomeownerId(homeownerId);
    }

    public List<Booking> getByProvider(Long providerId) {
        return bookingRepository.findByProviderId(providerId);
    }

    public Booking getById(Long id) {
        return getBookingOrThrow(id);
    }

    private Booking getBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
    }

    /** Guard for state machine transitions — throws if current status doesn't match expected. */
    private void assertStatus(Booking booking, BookingStatus expected) {
        if (booking.getStatus() != expected) {
            throw new BadRequestException(
                    "Expected booking status " + expected + " but found " + booking.getStatus());
        }
    }
}
