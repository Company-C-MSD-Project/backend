package com.example.FixItNow.repository;

import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByHomeownerId(Long homeownerId);

    List<Booking> findByProviderId(Long providerId);

    List<Booking> findByProviderIdAndStatus(Long providerId, BookingStatus status);

    List<Booking> findByHomeownerIdAndStatus(Long homeownerId, BookingStatus status);

    /** Count completed jobs for a provider — used in badge level calculation. */
    long countByProviderIdAndStatus(Long providerId, BookingStatus status);
}
