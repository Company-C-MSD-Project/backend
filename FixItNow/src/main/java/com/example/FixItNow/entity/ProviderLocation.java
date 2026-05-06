package com.fixitnow.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores the live GPS coordinates of a service provider (SRS FR9).
 * One row per provider — updated in real-time while a job is active.
 * Separate table keeps the hot-update path isolated from the main users table.
 */
@Entity
@Table(name = "provider_locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false, unique = true)
    private User provider;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void touch() {
        updatedAt = LocalDateTime.now();
    }
}
