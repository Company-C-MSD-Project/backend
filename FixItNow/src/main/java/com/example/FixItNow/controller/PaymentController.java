package com.example.FixItNow.controller;

import com.example.FixItNow.dto.response.ApiResponse;
import com.example.FixItNow.dto.response.PayPalOrderResponse;
import com.example.FixItNow.entity.Payment;
import com.example.FixItNow.service.PayPalPaymentService;
import com.example.FixItNow.service.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
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
    private final PayPalPaymentService payPalPaymentService;

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

    @PostMapping("/paypal/orders/{bookingId}")
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<ApiResponse<PayPalOrderResponse>> createPayPalOrder(@PathVariable Long bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "PayPal order created. Redirect the payer to approveUrl.",
                payPalPaymentService.createOrder(bookingId)));
    }

    @PostMapping("/paypal/orders/{orderId}/capture")
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<ApiResponse<Payment>> capturePayPalOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "PayPal order captured", payPalPaymentService.captureOrder(orderId)));
    }

    /** Public PayPal callback; authenticity is checked with PayPal before processing. */
    @PostMapping("/paypal/webhook")
    public ResponseEntity<ApiResponse<Payment>> payPalWebhook(
            @RequestHeader HttpHeaders requestHeaders,
            @RequestBody JsonNode event) {
        Map<String, String> headers = new HashMap<>();
        requestHeaders.forEach((name, values) -> {
            if (!values.isEmpty()) {
                headers.put(name.toLowerCase(), values.getFirst());
            }
        });
        Payment payment = payPalPaymentService.handleWebhook(event, headers);
        return ResponseEntity.ok(ApiResponse.ok(
                payment == null ? "PayPal event ignored" : "PayPal payment recorded", payment));
    }
}
