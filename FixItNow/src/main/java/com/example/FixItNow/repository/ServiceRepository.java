package com.example.FixItNow.repository;

import com.fixitnow.app.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByIsActiveTrue();

    List<Service> findByCategoryIdAndIsActiveTrue(Long categoryId);
}
