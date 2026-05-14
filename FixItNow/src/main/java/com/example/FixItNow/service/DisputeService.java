package com.example.FixItNow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import com.example.FixItNow.repository.DisputeRepository;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.UserRepository;
import com.example.FixItNow.entity.Dispute;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.DisputeStatus;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.exception.BadRequestException;

/**
 * Dispute management (SRS §2.1.2 Dispute Resolution module).
 * Users raise disputes; admin reviews and resolves them.
 * Escalated disputes trigger email alerts (SRS §2.1.4 Error Severity — Critical).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Dispute raise(Long bookingId, Long raisedById, String reason) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Prevent duplicate open disputes for the same booking
        if (disputeRepository.existsByBookingIdAndStatus(bookingId, DisputeStatus.OPEN)) {
            throw new BadRequestException("An open dispute already exists for this booking.");
        }

        User raisedBy = userRepository.findById(raisedById)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Dispute dispute = Dispute.builder()
                .booking(booking)
                .raisedBy(raisedBy)
                .reason(reason)
                .status(DisputeStatus.OPEN)
                .build();

        Dispute saved = disputeRepository.save(dispute);
        log.warn("Dispute raised for booking {} by user {}", bookingId, raisedById);

        // Notify admin team of new dispute
        userRepository.findByUserType(UserType.ADMIN).forEach(admin ->
                notificationService.send(admin, "DISPUTE_RAISED",
                        "New dispute raised for booking #" + bookingId + ": " + reason));

        return saved;
    }

    @Transactional
    public Dispute resolve(Long disputeId, Long adminId, String resolution) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolution(resolution);
        dispute.setResolvedBy(admin);
        dispute.setResolvedAt(LocalDateTime.now());

        Dispute saved = disputeRepository.save(dispute);

        // Notify both parties of resolution
        notificationService.send(dispute.getBooking().getHomeowner(), "DISPUTE_RESOLVED",
                "Your dispute for booking #" + dispute.getBooking().getId() + " has been resolved.");
        if (dispute.getBooking().getProvider() != null) {
            notificationService.send(dispute.getBooking().getProvider(), "DISPUTE_RESOLVED",
                    "Dispute for booking #" + dispute.getBooking().getId() + " has been resolved.");
        }
        return saved;
    }

    @Transactional
    public Dispute escalate(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));
        dispute.setStatus(DisputeStatus.ESCALATED);
        return disputeRepository.save(dispute);
    }

    public List<Dispute> getByStatus(DisputeStatus status) {
        return disputeRepository.findByStatus(status);
    }

    public List<Dispute> getAll() {
        return disputeRepository.findAll();
    }
}