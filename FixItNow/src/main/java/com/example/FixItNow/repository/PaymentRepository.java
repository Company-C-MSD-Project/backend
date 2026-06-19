package com.example.FixItNow.repository;

import com.example.FixItNow.entity.Payment;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByStripePaymentIntent(String paymentIntentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByPaypalOrderId(String paypalOrderId);
}
