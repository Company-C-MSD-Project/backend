package com.example.FixItNow.dto.response;

import com.example.FixItNow.entity.Payment;

public record PayPalOrderResponse(
        String orderId,
        String status,
        String approveUrl,
        Payment payment) {
}
