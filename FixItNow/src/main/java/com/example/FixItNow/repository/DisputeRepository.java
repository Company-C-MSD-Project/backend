package com.example.FixItNow.repository;

import com.fixitnow.app.entity.Dispute;
import com.fixitnow.app.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, Long> {

    List<Dispute> findByStatus(DisputeStatus status);

    List<Dispute> findByRaisedById(Long userId);

    boolean existsByBookingIdAndStatus(Long bookingId, DisputeStatus status);
}
