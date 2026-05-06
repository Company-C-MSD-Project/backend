package com.example.FixItNow.repository;

import com.example.FixItNow.entity.ProviderLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderLocationRepository extends JpaRepository<ProviderLocation, Long> {

    Optional<ProviderLocation> findByProviderId(Long providerId);
}
