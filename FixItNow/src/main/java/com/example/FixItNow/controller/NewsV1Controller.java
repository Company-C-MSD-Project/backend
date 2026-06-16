package com.example.FixItNow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.FixItNow.dto.v1.NewsArticleV1;
import com.example.FixItNow.service.NewsV1Service;

import lombok.RequiredArgsConstructor;

/**
 * News CMS (lib/news-admin-data.ts). GET is public; create/update/delete/upload are ADMIN.
 * Raw JSON bodies to match the fetch client.
 */
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsV1Controller {

    private final NewsV1Service newsService;

    @GetMapping
    public ResponseEntity<List<NewsArticleV1>> list() {
        return ResponseEntity.ok(newsService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsArticleV1> get(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsArticleV1> create(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.status(HttpStatus.CREATED).body(newsService.create(payload));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsArticleV1> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(newsService.update(id, payload));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        newsService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(newsService.uploadImage(file));
    }
}
