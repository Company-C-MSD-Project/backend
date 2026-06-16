package com.example.FixItNow.entity;

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
 * A provider's application to join the platform, reviewed by an admin
 * (AdminProviderRequestsPage). Created at provider signup; on approval the
 * linked provider User is verified. Status: "Pending" | "Approved" | "Rejected".
 */
@Entity
@Table(name = "provider_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The provider User created at signup (verified when this request is approved). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id")
    private User applicant;

    private String fullName;
    private String displayName;
    private String email;
    private String phone;
    private String nic;
    private String category;
    private String subSpeciality;
    private String district;
    private String experience;
    private String hourlyRate;
    private String availability;

    @Column(nullable = false)
    @lombok.Builder.Default
    private String status = "Pending";

    @lombok.Builder.Default
    private Integer score = 0;

    private String scoreLabel;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
