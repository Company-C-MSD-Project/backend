package com.example.FixItNow.controller;

import com.example.FixItNow.dto.response.ApiResponse;
import com.example.FixItNow.entity.Payment;
import com.example.FixItNow.service.PaymentService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Payment endpoints (SRS FR10).
 * POST /api/payments/initiate/{bookingId} — create Stripe PaymentIntent
 * POST /api/payments/confirm             — webhook callback from Stripe
 * POST /api/payments/refund/{paymentId}  — process refund (admin only)
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate/{bookingId}")
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<ApiResponse<Payment>> initiate(@PathVariable Long bookingId) throws StripeException {
        Payment payment = paymentService.initiatePayment(bookingId);
        return ResponseEntity.ok(ApiResponse.ok("Payment initiated. Use the Stripe client secret to confirm.", payment));
    }

    /** Stripe sends this webhook when a payment succeeds. */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Payment>> confirm(@RequestParam String paymentIntentId) {
        Payment payment = paymentService.confirmPayment(paymentIntentId);
        return ResponseEntity.ok(ApiResponse.ok("Payment confirmed", payment));
    }

    @PostMapping("/refund/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Payment>> refund(@PathVariable Long paymentId) throws StripeException {
        Payment payment = paymentService.refund(paymentId);
        return ResponseEntity.ok(ApiResponse.ok("Refund processed", payment));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<Payment>> getByBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.ok("Payment found", paymentService.getByBookingId(bookingId)));
    }
}
