package com.example.FixItNow.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** News/blog article (NewsPage + AdminNewsEditorPage). Status: "live" | "draft" | "scheduled". */
@Entity
@Table(name = "news_articles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(columnDefinition = "LONGTEXT")
    private String body;

    private String category;
    private String tag;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(nullable = false)
    @lombok.Builder.Default
    private String status = "draft";

    /** ISO timestamp string for scheduled publishing; null for immediate/live. */
    @Column(name = "publish_at")
    private String publishAt;

    @Column(name = "author_id")
    private Long authorId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
