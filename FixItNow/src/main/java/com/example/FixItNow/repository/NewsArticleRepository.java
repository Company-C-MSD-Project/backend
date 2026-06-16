package com.example.FixItNow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.FixItNow.entity.NewsArticle;

@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    List<NewsArticle> findAllByOrderByUpdatedAtDesc();

    List<NewsArticle> findByStatusOrderByUpdatedAtDesc(String status);
}
