package com.example.FixItNow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.FixItNow.entity.ProviderRequest;

@Repository
public interface ProviderRequestRepository extends JpaRepository<ProviderRequest, Long> {

    List<ProviderRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
