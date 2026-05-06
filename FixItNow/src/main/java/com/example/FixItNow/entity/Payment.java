package com.fixitnow.app.entity;

import com.fixitnow.app.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment record tied to a booking (SRS FR10).
 * Stripe PaymentIntent ID stored for webhook reconciliation.
 * All payment transactions follow ACID properties (SRS §2.1.2 constraints).
 */
@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** One payment per booking — enforced by unique constraint. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String method = "STRIPE";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    /** Stripe PaymentIntent ID — used to confirm or refund via Stripe API. */
    @Column(name = "stripe_payment_intent")
    private String stripePaymentIntent;

    @Column(name = "transaction_ref")
    private String transactionRef;

    /** Filesystem path to the auto-generated PDF invoice. */
    @Column(name = "invoice_path")
    private String invoicePath;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
