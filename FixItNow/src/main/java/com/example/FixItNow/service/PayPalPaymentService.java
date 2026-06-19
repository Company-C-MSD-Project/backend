package com.example.FixItNow.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.dto.response.PayPalOrderResponse;
import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.Payment;
import com.example.FixItNow.entity.WalletTransaction;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.PaymentStatus;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.exception.ExternalServiceException;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.PaymentRepository;
import com.example.FixItNow.repository.WalletTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalPaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final NotificationService notificationService;
    private final PayPalClient payPalClient;

    @Value("${paypal.currency:USD}")
    private String currency;

    @Value("${paypal.webhook-id:}")
    private String webhookId;

    @Transactional
    public PayPalOrderResponse createOrder(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Payment can only be made for completed bookings.");
        }
        if (booking.getProvider() == null) {
            throw new BadRequestException("Booking has no assigned provider.");
        }
        if (paymentRepository.findByBookingId(bookingId).isPresent()) {
            throw new BadRequestException("Payment already initiated for this booking.");
        }

        BigDecimal amount = booking.getFinalCost() != null
                ? booking.getFinalCost() : booking.getEstimatedCost();
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Booking payment amount must be greater than zero.");
        }

        JsonNode order = payPalClient.createOrder(bookingId, amount, currency);
        String orderId = requiredText(order, "id");
        String status = order.path("status").asText();
        String approveUrl = null;
        for (JsonNode link : order.path("links")) {
            if ("approve".equals(link.path("rel").asText())
                    || "payer-action".equals(link.path("rel").asText())) {
                approveUrl = link.path("href").asText();
                break;
            }
        }

        Payment payment = paymentRepository.save(Payment.builder()
                .booking(booking)
                .amount(amount)
                .method("PAYPAL")
                .paypalOrderId(orderId)
                .status(PaymentStatus.PENDING)
                .build());
        return new PayPalOrderResponse(orderId, status, approveUrl, payment);
    }

    @Transactional
    public Payment captureOrder(String orderId) {
        Payment existing = findByOrderId(orderId);
        if (existing.getStatus() == PaymentStatus.COMPLETED) {
            return existing;
        }

        JsonNode captured = payPalClient.captureOrder(orderId);
        if (!"COMPLETED".equals(captured.path("status").asText())) {
            throw new ExternalServiceException("PayPal order was not completed after capture");
        }
        String captureId = captured.path("purchase_units").path(0)
                .path("payments").path("captures").path(0).path("id").asText(null);
        return completePayment(orderId, captureId);
    }

    @Transactional
    public Payment handleWebhook(JsonNode event, Map<String, String> headers) {
        if (webhookId == null || webhookId.isBlank()) {
            throw new ExternalServiceException("PAYPAL_WEBHOOK_ID is not configured");
        }
        if (!payPalClient.verifyWebhook(headers, event, webhookId)) {
            throw new BadRequestException("Invalid PayPal webhook signature.");
        }
        if (!"PAYMENT.CAPTURE.COMPLETED".equals(event.path("event_type").asText())) {
            return null;
        }

        JsonNode resource = event.path("resource");
        String orderId = resource.path("supplementary_data").path("related_ids")
                .path("order_id").asText(null);
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("PayPal webhook did not include an order ID.");
        }
        return completePayment(orderId, resource.path("id").asText(null));
    }

    protected Payment completePayment(String orderId, String captureId) {
        Payment payment = findByOrderId(orderId);
        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            return payment;
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setPaypalCaptureId(captureId);
        payment.setTransactionRef(captureId != null ? captureId : "PAYPAL-" + orderId);
        payment.setInvoiceNo("INV-" + payment.getBooking().getId() + "-" + System.currentTimeMillis());
        Payment saved = paymentRepository.save(payment);

        String walletReference = "PAYPAL-" + orderId;
        if (!walletTransactionRepository.existsByReference(walletReference)) {
            walletTransactionRepository.save(WalletTransaction.builder()
                    .user(payment.getBooking().getProvider())
                    .description("PayPal payment for booking #" + payment.getBooking().getId())
                    .reference(walletReference)
                    .type("Job payment")
                    .tone("credit")
                    .amount(payment.getAmount())
                    .build());
        }

        notificationService.send(payment.getBooking().getHomeowner(), "PAYMENT_COMPLETED",
                "PayPal payment of " + currency + " " + payment.getAmount()
                        + " confirmed for booking #" + payment.getBooking().getId());
        notificationService.send(payment.getBooking().getProvider(), "PAYMENT_RECEIVED",
                "PayPal payment received for booking #" + payment.getBooking().getId()
                        + ". Amount: " + currency + " " + payment.getAmount());
        log.info("PayPal payment captured for booking {} (order {})",
                payment.getBooking().getId(), orderId);
        return saved;
    }

    private Payment findByOrderId(String orderId) {
        return paymentRepository.findByPaypalOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment found for PayPal order: " + orderId));
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new ExternalServiceException("PayPal response did not include " + field);
        }
        return value;
    }
}
