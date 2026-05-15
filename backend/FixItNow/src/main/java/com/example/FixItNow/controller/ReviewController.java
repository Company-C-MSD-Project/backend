package com.fixitnow.app.controller;

import com.fixitnow.app.dto.response.ApiResponse;
import com.fixitnow.app.entity.Review;
import com.fixitnow.app.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Review and rating endpoints (SRS FR11). */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("hasRole('HOMEOWNER')")
    public ResponseEntity<ApiResponse<Review>> submit(
            @RequestParam Long homeownerId,
            @RequestParam Long bookingId,
            @RequestParam int rating,
            @RequestParam(required = false) String comment) {
        Review review = reviewService.submitReview(homeownerId, bookingId, rating, comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Review submitted", review));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<ApiResponse<List<Review>>> getProviderReviews(@PathVariable Long providerId) {
        return ResponseEntity.ok(ApiResponse.ok("Reviews retrieved", reviewService.getProviderReviews(providerId)));
    }
}
