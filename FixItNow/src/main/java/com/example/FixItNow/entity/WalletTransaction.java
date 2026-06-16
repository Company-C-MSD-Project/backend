package com.example.FixItNow.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persisted wallet movements that are NOT derivable from bookings — primarily
 * provider withdrawals. Job earnings are derived from completed bookings at read
 * time, so they are not stored here (avoids double-counting / drift).
 */
@Entity
@Table(name = "wallet_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String description;

    @Column(unique = true)
    private String reference;

    /** e.g. "withdrawal". */
    private String type;

    /** UI tone: "credit" | "transfer" | "fee". */
    private String tone;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
