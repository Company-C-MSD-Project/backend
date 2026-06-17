package com.example.FixItNow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.FixItNow.entity.ProviderServiceCard;

@Repository
public interface ProviderServiceRepository extends JpaRepository<ProviderServiceCard, Long> {

    List<ProviderServiceCard> findByProviderIdOrderByCreatedAtDesc(Long providerId);
}
