package com.example.FixItNow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;

import com.example.FixItNow.enums.NotificationChannel;

import java.time.LocalDateTime;

/** In-app and email notifications dispatched on booking/payment events (SRS FR12). */
@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Notification type key, e.g. BOOKING_CONFIRMED, PAYMENT_RECEIVED, DISPUTE_RAISED. */
    @Column(nullable = false, length = 80)
    private String type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @lombok.Builder.Default
    private NotificationChannel channel = NotificationChannel.BOTH;

    @Column(name = "is_read")
    @lombok.Builder.Default
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;
}
