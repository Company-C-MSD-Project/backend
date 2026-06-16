package com.example.FixItNow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.FixItNow.entity.CategoryRequest;

@Repository
public interface CategoryRequestRepository extends JpaRepository<CategoryRequest, Long> {

    List<CategoryRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
