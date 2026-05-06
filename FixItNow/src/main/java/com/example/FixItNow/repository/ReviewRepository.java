package com.example.FixItNow.repository;

import com.example.FixItNow.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProviderId(Long providerId);

    Optional<Review> findByBookingId(Long bookingId);

    boolean existsByBookingId(Long bookingId);

    /** Average rating for a provider — used to update badge level after each review. */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.provider.id = :providerId")
    Optional<Double> findAverageRatingByProviderId(@Param("providerId") Long providerId);
}
