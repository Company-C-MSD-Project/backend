package com.example.FixItNow.unit;

import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.Review;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.ReviewRepository;
import com.example.FixItNow.repository.UserRepository;
import com.example.FixItNow.service.ReviewService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReviewService — FR11 (reviews and ratings).
 * Tests valid submission, out-of-range rating validation, and the
 * "booking must be completed" gate.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Unit Tests")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ReviewService reviewService;

    private User homeowner;
    private User provider;
    private Booking completedBooking;
    private Booking pendingBooking;

    @BeforeEach
    void setUp() {
        homeowner = User.builder()
                .id(1L).name("Alice").email("alice@test.com")
                .username("alice").passwordHash("hash")
                .userType(UserType.HOMEOWNER).rating(0.0).build();

        provider = User.builder()
                .id(2L).name("Bob Provider").email("bob@test.com")
                .username("bob").passwordHash("hash")
                .userType(UserType.SERVICE_PROVIDER).rating(0.0).build();

        completedBooking = Booking.builder()
                .id(10L).homeowner(homeowner).provider(provider)
                .status(BookingStatus.COMPLETED)
                .scheduledDate(LocalDateTime.now().minusDays(1))
                .build();

        pendingBooking = Booking.builder()
                .id(11L).homeowner(homeowner).provider(provider)
                .status(BookingStatus.PENDING)
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .build();
    }

    // UT-26: Submit valid review (rating 1–5) → review saved, badge recalculated
    @Test
    @DisplayName("UT-26 | submitReview with valid rating saves review and recalculates badge")
    void ut26_submitValidReview_savesReviewAndRecalculatesBadge() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(completedBooking));
        when(reviewRepository.existsByBookingId(10L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        // Badge recalculation dependencies
        when(userRepository.findById(2L)).thenReturn(Optional.of(provider));
        when(reviewRepository.findAverageRatingByProviderId(2L)).thenReturn(Optional.of(4.5));
        when(bookingRepository.countByProviderIdAndStatus(2L, BookingStatus.COMPLETED)).thenReturn(110L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Review result = reviewService.submitReview(1L, 10L, 5, "Excellent work!");

        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Excellent work!");
        verify(reviewRepository).save(any(Review.class));
        verify(userRepository).save(provider); // badge update happened
    }

    // UT-27: Rating outside 1–5 is rejected at the entity Bean Validation level
    @Test
    @DisplayName("UT-27 | Rating value 0 or 6 violates @Min(1)/@Max(5) constraint")
    void ut27_ratingOutOfRange_violatesBeanValidation() {
        // Validate the Review entity directly with Jakarta Validator
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            Review tooLow = Review.builder()
                    .booking(completedBooking).homeowner(homeowner).provider(provider)
                    .rating(0).comment("Too low").build();

            Review tooHigh = Review.builder()
                    .booking(completedBooking).homeowner(homeowner).provider(provider)
                    .rating(6).comment("Too high").build();

            assertThat(validator.validate(tooLow)).isNotEmpty();
            assertThat(validator.validate(tooHigh)).isNotEmpty();
        }
    }

    // UT-28: Review is blocked if booking is not yet COMPLETED → BadRequestException
    @Test
    @DisplayName("UT-28 | submitReview on non-completed booking throws BadRequestException")
    void ut28_reviewBlockedForNonCompletedBooking_throwsBadRequest() {
        when(bookingRepository.findById(11L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> reviewService.submitReview(1L, 11L, 4, "Good"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("completed bookings");

        verify(reviewRepository, never()).save(any());
    }
}
