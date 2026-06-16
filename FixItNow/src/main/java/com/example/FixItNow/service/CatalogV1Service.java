package com.example.FixItNow.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.dto.v1.CategoryV1;
import com.example.FixItNow.dto.v1.ProviderV1;
import com.example.FixItNow.dto.v1.SubServiceV1;
import com.example.FixItNow.entity.Category;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.BadgeLevel;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.CategoryRepository;
import com.example.FixItNow.repository.ServiceRepository;
import com.example.FixItNow.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Read-side adapter that maps the backend catalog (Category / Service / provider User)
 * to the Supabase-shaped browse DTOs the frontend expects (lib/booking.ts).
 */
@Service
@RequiredArgsConstructor
public class CatalogV1Service {

    private final CategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Transactional(readOnly = true)
    public List<CategoryV1> listCategories() {
        return categoryRepository.findByIsActiveTrue().stream()
                .map(this::toCategory)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubServiceV1> listSubServices(Long categoryId) {
        return serviceRepository.findByCategoryIdAndIsActiveTrue(categoryId).stream()
                .map(this::toSubService)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubServiceV1 getSubService(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .map(this::toSubService)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + serviceId));
    }

    @Transactional(readOnly = true)
    public List<ProviderV1> listProviders(Long categoryId, Boolean availableOnly) {
        final List<User> providers;
        final String categoryIdStr;
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
            providers = userRepository.findVerifiedProvidersByCategory(category.getCategoryType());
            categoryIdStr = String.valueOf(category.getId());
        } else {
            providers = userRepository.findByUserType(UserType.SERVICE_PROVIDER).stream()
                    .filter(User::isVerified)
                    .filter(User::isActive)
                    .toList();
            categoryIdStr = null;
        }
        return providers.stream()
                .filter(p -> !Boolean.TRUE.equals(availableOnly) || isAvailable(p))
                .map(p -> toProvider(p, categoryIdStr))
                .toList();
    }

    // ----- mappers -----

    private CategoryV1 toCategory(Category c) {
        long pros = userRepository.findVerifiedProvidersByCategory(c.getCategoryType()).size();
        return CategoryV1.builder()
                .id(String.valueOf(c.getId()))
                .slug(slugify(c.getCategoryType()))
                .name(c.getCategoryType())
                .icon(null)
                .prosCount(pros)
                .build();
    }

    private SubServiceV1 toSubService(com.example.FixItNow.entity.Service s) {
        String categoryId = s.getCategory() != null ? String.valueOf(s.getCategory().getId()) : null;
        return SubServiceV1.builder()
                .id(String.valueOf(s.getId()))
                .categoryId(categoryId)
                .slug(slugify(s.getName()))
                .name(s.getName())
                .description(s.getDescription())
                .icon(null)
                .basePrice(s.getDayPayment())
                .build();
    }

    private ProviderV1 toProvider(User u, String categoryIdStr) {
        long jobsDone = bookingRepository.countByProviderIdAndStatus(u.getId(), BookingStatus.COMPLETED);
        String resolvedCategoryId = categoryIdStr != null ? categoryIdStr : resolveCategoryId(u.getServiceCategory());
        ProviderV1.ProfileV1 profile = ProviderV1.ProfileV1.builder()
                .id(String.valueOf(u.getId()))
                .displayName(u.getName())
                .username(u.getUsername())
                .avatarUrl(null)
                .build();
        return ProviderV1.builder()
                .id(String.valueOf(u.getId()))
                .headline(u.getServiceCategory() != null ? u.getServiceCategory() + " Specialist" : null)
                .categoryId(resolvedCategoryId)
                .rating(u.getRating() != null ? u.getRating() : 0.0)
                .jobsDone(jobsDone)
                .yearsExperience(0)
                .hourlyRate(0.0)
                .city(null)
                .distanceKm(null)
                .verified(u.isVerified())
                .topRated(u.getBadgeLevel() == BadgeLevel.TOP_RATED)
                .available(isAvailable(u))
                .bio(null)
                .profile(profile)
                .build();
    }

    private boolean isAvailable(User u) {
        String s = u.getAvailabilityStatus();
        return s == null || s.isBlank() || "available".equalsIgnoreCase(s);
    }

    private String resolveCategoryId(String serviceCategory) {
        if (serviceCategory == null) return null;
        return categoryRepository.findByIsActiveTrue().stream()
                .filter(c -> serviceCategory.equalsIgnoreCase(c.getCategoryType()))
                .map(c -> String.valueOf(c.getId()))
                .findFirst()
                .orElse(null);
    }

    private String slugify(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
