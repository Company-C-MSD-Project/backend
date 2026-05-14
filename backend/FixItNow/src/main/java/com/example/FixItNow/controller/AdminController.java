package com.example.FixItNow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.response.ApiResponse;
import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.PaymentRepository;
import com.example.FixItNow.service.UserService;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin-only endpoints (SRS FR3, FR6, §2.1.2 Admin Use Case).
 * All routes are protected by @PreAuthorize("hasRole('ADMIN')")
 * in addition to the SecurityConfig path matcher for /api/admin/**.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    /** Verify a service provider after reviewing their credentials (SRS §2.1.2 Admin Use Case). */
    @PostMapping("/providers/{id}/verify")
    public ResponseEntity<ApiResponse<User>> verifyProvider(@PathVariable Long id) {
        User provider = userService.verifyProvider(id);
        provider.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.ok("Provider verified", provider));
    }

    /** Suspend/unsuspend a user account (SRS FR3). */
    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<User>> setUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        User user = userService.setActiveStatus(id, active);
        user.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.ok("User status updated", user));
    }

    /** Blacklist a homeowner who violates platform rules. */
    @PutMapping("/users/{id}/blacklist")
    public ResponseEntity<ApiResponse<User>> blacklist(@PathVariable Long id) {
        User user = userService.blacklistHomeowner(id);
        user.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.ok("User blacklisted", user));
    }

    /** Platform analytics for admin dashboard (SRS FR6). */
    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers",     userService.getAllUsers().size());
        stats.put("totalHomeowners",userService.getUsersByType(UserType.HOMEOWNER).size());
        stats.put("totalProviders", userService.getUsersByType(UserType.SERVICE_PROVIDER).size());
        stats.put("totalBookings",  bookingRepository.count());
        stats.put("totalPayments",  paymentRepository.count());
        return ResponseEntity.ok(ApiResponse.ok("Analytics data", stats));
    }

    /** Pending provider verifications list. */
    @GetMapping("/providers/pending")
    public ResponseEntity<ApiResponse<List<User>>> getPendingProviders() {
        List<User> pending = userService.getUsersByType(UserType.SERVICE_PROVIDER)
                .stream()
                .filter(u -> !u.isVerified())
                .peek(u -> u.setPasswordHash(null))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Pending verifications", pending));
    }

    /** All bookings list for admin monitoring. */
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings() {
        return ResponseEntity.ok(ApiResponse.ok("All bookings", bookingRepository.findAll()));
    }
}
