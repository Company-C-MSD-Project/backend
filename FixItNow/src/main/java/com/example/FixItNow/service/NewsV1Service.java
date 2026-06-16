package com.example.FixItNow.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.FixItNow.dto.v1.NewsArticleV1;
import com.example.FixItNow.entity.NewsArticle;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.repository.NewsArticleRepository;

import lombok.RequiredArgsConstructor;

/** News CMS (lib/news-admin-data.ts). Public read of live articles; admin write + image upload. */
@Service
@RequiredArgsConstructor
public class NewsV1Service {

    private final NewsArticleRepository repository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public List<NewsArticleV1> list() {
        return repository.findAllByOrderByUpdatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public NewsArticleV1 get(Long id) {
        return toDto(require(id));
    }

    @Transactional
    public NewsArticleV1 create(Map<String, Object> payload) {
        String title = str(payload, "title");
        if (title == null || title.isBlank()) throw new BadRequestException("Title is required.");
        NewsArticle a = NewsArticle.builder()
                .title(title)
                .excerpt(str(payload, "excerpt"))
                .body(str(payload, "body"))
                .category(str(payload, "category"))
                .tag(str(payload, "tag"))
                .imageUrl(str(payload, "image_url", "imageUrl"))
                .status(orDefault(str(payload, "status"), "draft"))
                .publishAt(str(payload, "publish_at", "publishAt"))
                .build();
        return toDto(repository.save(a));
    }

    @Transactional
    public NewsArticleV1 update(Long id, Map<String, Object> payload) {
        NewsArticle a = require(id);
        if (payload.containsKey("title")) a.setTitle(str(payload, "title"));
        if (payload.containsKey("excerpt")) a.setExcerpt(str(payload, "excerpt"));
        if (payload.containsKey("body")) a.setBody(str(payload, "body"));
        if (payload.containsKey("category")) a.setCategory(str(payload, "category"));
        if (payload.containsKey("tag")) a.setTag(str(payload, "tag"));
        if (payload.containsKey("image_url") || payload.containsKey("imageUrl"))
            a.setImageUrl(str(payload, "image_url", "imageUrl"));
        if (payload.containsKey("status")) a.setStatus(str(payload, "status"));
        if (payload.containsKey("publish_at") || payload.containsKey("publishAt"))
            a.setPublishAt(str(payload, "publish_at", "publishAt"));
        return toDto(repository.save(a));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(require(id));
    }

    /** Stores the uploaded image under the configured upload dir; returns its public URL. */
    public Map<String, Object> uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("No file provided.");
        try {
            Path dir = Paths.get(uploadDir, "news");
            Files.createDirectories(dir);
            String ext = extensionOf(file.getOriginalFilename());
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Files.copy(file.getInputStream(), dir.resolve(filename));
            String url = "/uploads/news/" + filename;
            return Map.of("url", url, "public_url", url);
        } catch (IOException e) {
            throw new BadRequestException("Failed to store image: " + e.getMessage());
        }
    }

    // ----- helpers -----

    private NewsArticle require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("News article not found: " + id));
    }

    private NewsArticleV1 toDto(NewsArticle a) {
        return NewsArticleV1.builder()
                .id(String.valueOf(a.getId()))
                .title(a.getTitle())
                .excerpt(a.getExcerpt())
                .body(a.getBody())
                .category(a.getCategory())
                .tag(a.getTag())
                .imageUrl(a.getImageUrl())
                .status(a.getStatus())
                .publishAt(a.getPublishAt())
                .createdAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : null)
                .updatedAt(a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null)
                .build();
    }

    private String extensionOf(String name) {
        if (name == null) return ".jpg";
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".jpg";
    }

    private String orDefault(String v, String def) {
        return v != null && !v.isBlank() ? v : def;
    }

    private String str(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return String.valueOf(v);
        }
        return null;
    }
}
