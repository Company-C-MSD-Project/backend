package com.example.FixItNow.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A service provider's self-managed listing on their "My Service Cards" page
 * (ProviderServiceCardsPage / services/services.ts). Distinct from the admin-curated
 * {@link Service} catalog (/api/services) — that entity is a shared, admin-owned
 * service definition; this one is owned by and scoped to a single SERVICE_PROVIDER.
 */
@Entity
@Table(name = "provider_service_cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderServiceCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @Column(nullable = false)
    private String title;

    private String category;

    /** e.g. "hourly" | "fixed". Free text — not enumerated on the frontend either. */
    @Column(name = "rate_type")
    private String rateType;

    @Column(name = "rate_amount", precision = 12, scale = 2)
    private BigDecimal rateAmount;

    @Column(name = "min_fee", precision = 12, scale = 2)
    private BigDecimal minFee;

    @Column(name = "short_summary", length = 500)
    private String shortSummary;

    @Column(name = "full_description", columnDefinition = "TEXT")
    private String fullDescription;

    /** Free text, e.g. "2-3 hours". */
    private String duration;

    /** Free text, e.g. "30-day warranty". */
    private String warranty;

    @Column(name = "cover_image", length = 1024)
    private String coverImage;

    /** "draft" | "published" | "hidden" — set independently of {@link #published}. */
    @Column(nullable = false)
    @lombok.Builder.Default
    private String status = "draft";

    /**
     * Comma-separated district names served by this card (e.g. "Colombo,Gampaha,Negombo").
     * Stored flat rather than as a JPA @ElementCollection to match this codebase's existing
     * convention for list-ish text fields (see User.preferredServices, User.certifications).
     */
    @Column(columnDefinition = "TEXT")
    private String districts;

    /** Card icon, e.g. "🔧". */
    private String emoji;

    /** Card background token/class used by the frontend, e.g. "bg-blue-100". */
    private String bg;

    /** Freeform display price text shown on the card, e.g. "From Rs 3,000/hr". */
    @Column(name = "price_line")
    private String priceLine;

    /** Quick visibility toggle, set together with {@link #status} by togglePublish. */
    @Column(nullable = false)
    @lombok.Builder.Default
    private boolean published = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
