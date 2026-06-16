package com.example.FixItNow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.enums.BadgeLevel;


/**
 * User entity shared by Homeowners, ServiceProviders, and Admins. A single table
 * holds all role-specific columns, with the role stored in the {@code user_type}
 * column — this keeps role-check-heavy endpoints join-free (SRS §2.5 Class Diagram).
 *
 * Note: this is a plain entity, NOT a JPA inheritance hierarchy. user_type is a
 * normal persisted column, so the role is written on insert and read back correctly.
 */
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt hashed password — never stored in plain text. */
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    private String phone;
    private String address;

    @Lob
    private byte[] photo;

    @Column(name = "is_active")
    @lombok.Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ---- Homeowner-specific columns ----
    @Column(name = "is_blacklisted")
    @lombok.Builder.Default
    private boolean isBlacklisted = false;

    @Column(columnDefinition = "TEXT")
    private String preferredServices;

    // ---- ServiceProvider-specific columns ----
    @Column(name = "service_category")
    private String serviceCategory;

    @Column(columnDefinition = "TEXT")
    private String certifications;

    @Column(name = "is_verified")
    @lombok.Builder.Default
    private boolean isVerified = false;

    @Column(name = "availability_status")
    private String availabilityStatus;

    @Column
    @lombok.Builder.Default
    private Double rating = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_level")
    @lombok.Builder.Default
    private BadgeLevel badgeLevel = BadgeLevel.NONE;

    // ---- Admin-specific columns ----
    private String department;

    @Column(name = "access_level")
    @lombok.Builder.Default
    private Integer accessLevel = 1;
}
