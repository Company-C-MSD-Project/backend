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
 * A request to add a new service category (AdminCategoryRequestsPage). On approval
 * (status "Active") a matching catalog Category is created. Status: "Pending" |
 * "Active" | "Rejected".
 */
@Entity
@Table(name = "category_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String icon;

    @Column(nullable = false)
    private String name;

    private String subtitle;
    private String requestedBy;
    private String requesterEmail;
    private String contact;

    @lombok.Builder.Default
    private Integer providersWaiting = 0;

    @Column(nullable = false)
    @lombok.Builder.Default
    private String status = "Pending";

    private String priceRange;
    private String platformFee;
    private String demand;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
