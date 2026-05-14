package com.example.FixItNow.repository;

import com.example.FixItNow.entity.Dispute;
import com.example.FixItNow.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    List<Dispute> findByStatus(DisputeStatus status);

    List<Dispute> findByRaisedById(Long userId);

    boolean existsByBookingIdAndStatus(Long bookingId, DisputeStatus status);
}
