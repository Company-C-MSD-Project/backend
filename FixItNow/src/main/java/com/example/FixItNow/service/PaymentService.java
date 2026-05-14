package com.example.FixItNow.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.repository.PaymentRepository;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.entity.Payment;
import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.PaymentStatus;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.exception.BadRequestException;

import java.time.LocalDateTime;

/**
 * Stripe payment processing (SRS FR10).
 * Flow: initiate (creates Stripe PaymentIntent) → confirm (on successful charge) → invoice.
 * All DB writes are in @Transactional; Stripe API calls are not
 * (Stripe operations can't be rolled back with DB transactions).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Creates a Stripe PaymentIntent for the booking amount.
     * Returns the client_secret that the frontend uses to confirm payment.
     */
    @Transactional
    public Payment initiatePayment(Long bookingId) throws StripeException {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Payment can only be made for completed bookings.");
        }
        if (paymentRepository.findByBookingId(bookingId).isPresent()) {
            throw new BadRequestException("Payment already initiated for this booking.");
        }

        // Stripe amounts are in the smallest currency unit (cents)
        long amountCents = booking.getEstimatedCost().multiply(java.math.BigDecimal.valueOf(100)).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountCents)
                .setCurrency("usd")
                .putMetadata("bookingId", bookingId.toString())
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getEstimatedCost())
                .stripePaymentIntent(intent.getId())
                .status(PaymentStatus.PENDING)
                .build();

        log.info("Payment initiated for booking {} — PaymentIntent: {}", bookingId, intent.getId());
        return paymentRepository.save(payment);
    }

    /** Called after Stripe webhook confirms successful charge. */
    @Transactional
    public Payment confirmPayment(String stripePaymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntent(stripePaymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No payment found for intent: " + stripePaymentIntentId));

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setTransactionRef("TXN-" + System.currentTimeMillis());
        Payment saved = paymentRepository.save(payment);

        // Notify both parties
        notificationService.send(payment.getBooking().getHomeowner(), "PAYMENT_COMPLETED",
                "Payment of $" + payment.getAmount() + " confirmed for booking #"
                + payment.getBooking().getId());
        notificationService.send(payment.getBooking().getProvider(), "PAYMENT_RECEIVED",
                "Payment received for booking #" + payment.getBooking().getId()
                + ". Amount: $" + payment.getAmount());

        log.info("Payment confirmed for booking {}", payment.getBooking().getId());
        return saved;
    }

    /** Process a refund through Stripe (SRS FR10). */
    @Transactional
    public Payment refund(Long paymentId) throws StripeException {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("Only completed payments can be refunded.");
        }

        com.stripe.model.Refund.create(
                RefundCreateParams.builder()
                        .setPaymentIntent(payment.getStripePaymentIntent())
                        .build());

        payment.setStatus(PaymentStatus.REFUNDED);
        return paymentRepository.save(payment);
    }

    public Payment getByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment for booking: " + bookingId));
    }
}

