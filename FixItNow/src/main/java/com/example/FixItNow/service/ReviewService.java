package com.example.FixItNow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Post-service review and reputation badge management (SRS FR11, §2.1.2 Reputation System).
 * After each review, the provider's average rating and badge level are recalculated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Transactional
    public Review submitReview(Long homeownerId, Long bookingId, int rating, String comment) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Reviews can only be submitted for completed bookings.");
        }
        if (reviewRepository.existsByBookingId(bookingId)) {
            throw new BadRequestException("A review has already been submitted for this booking.");
        }
        if (!booking.getHomeowner().getId().equals(homeownerId)) {
            throw new BadRequestException("You can only review bookings you made.");
        }

        Review review = Review.builder()
                .booking(booking)
                .homeowner(booking.getHomeowner())
                .provider(booking.getProvider())
                .rating(rating)
                .comment(comment)
                .build();

        Review saved = reviewRepository.save(review);

        // Recalculate provider's average rating and update badge immediately
        recalculateBadge(booking.getProvider().getId());
        log.info("Review submitted for provider {} — rating: {}", booking.getProvider().getId(), rating);

        return saved;
    }

    public List<Review> getProviderReviews(Long providerId) {
        return reviewRepository.findByProviderId(providerId);
    }

    /**
     * Recalculates provider badge level based on average rating and completed job count.
     * Badge thresholds from SRS BadgeLevel enum comments.
     */
    @Transactional
    public void recalculateBadge(Long providerId) {
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        double avgRating   = reviewRepository.findAverageRatingByProviderId(providerId).orElse(0.0);
        long completedJobs = bookingRepository.countByProviderIdAndStatus(providerId, BookingStatus.COMPLETED);

        provider.setRating(Math.round(avgRating * 100.0) / 100.0);
        provider.setBadgeLevel(determineBadge(avgRating, completedJobs));
        userRepository.save(provider);
    }

    private BadgeLevel determineBadge(double avgRating, long completedJobs) {
        if (avgRating >= 4.5 && completedJobs >= 100) return BadgeLevel.TOP_RATED;
        if (avgRating >= 4.0 && completedJobs >= 50)  return BadgeLevel.GOLD;
        if (avgRating >= 3.5 && completedJobs >= 20)  return BadgeLevel.SILVER;
        if (avgRating >= 3.0 && completedJobs >= 5)   return BadgeLevel.BRONZE;
        return BadgeLevel.NONE;
    }
}
