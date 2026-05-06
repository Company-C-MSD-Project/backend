package com.example.FixItNow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.example.FixItNow.enums.BookingStatus;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Central booking entity — connects homeowner, provider, and service.
 * Status transitions enforced in BookingService (SRS §2.1.2 State-Transition Diagram):
 * PENDING → ACCEPTED → IN_PROGRESS → COMPLETED
 * PENDING / ACCEPTED → CANCELLED
 */
@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Homeowner who created the booking. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homeowner_id", nullable = false)
    private User homeowner;

    /** Provider assigned after smart matching — null until ACCEPTED. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private User provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "booking_type")
    private String bookingType;

    /** Homeowner's description of the problem/issue. */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    /** Automated cost estimate before confirmation (SRS FR8). */
    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "final_cost", precision = 10, scale = 2)
    private BigDecimal finalCost;

    /** Service location address provided by homeowner. */
    @Column(columnDefinition = "TEXT")
    private String serviceAddress;

    /** GPS latitude of service location. */
    @Column(name = "service_latitude")
    private Double serviceLatitude;

    /** GPS longitude of service location. */
    @Column(name = "service_longitude")
    private Double serviceLongitude;

    /** Estimated time of arrival in minutes (SRS FR9). */
    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @lombok.Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
