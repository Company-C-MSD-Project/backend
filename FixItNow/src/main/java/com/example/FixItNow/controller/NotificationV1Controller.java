package com.example.FixItNow.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.FixItNow.service.NotificationV1Service;
import com.example.FixItNow.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/**
 * Current-user notifications (lib/booking.ts, lib/notifications-data.ts):
 * /api/v1/notifications, /api/v1/notifications/mark-read, /api/v1/notifications/stream.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationV1Controller {

    private final NotificationV1Service notificationService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(name = "unread", required = false, defaultValue = "false") boolean unread) {
        return ResponseEntity.ok(notificationService.list(SecurityUtil.getCurrentUserId(), unread));
    }

    /**
     * With a body of {@code {"ids": [1, 2]}}, marks only those notifications read.
     * With no body (or no "ids" key), falls back to marking all of the user's notifications read.
     */
    @PostMapping("/mark-read")
    public ResponseEntity<Map<String, Object>> markRead(
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Long> ids = extractIds(body);
        if (ids != null) {
            return ResponseEntity.ok(notificationService.markRead(userId, ids));
        }
        return ResponseEntity.ok(notificationService.markAllRead(userId));
    }

    /** Realtime feed for NotificationsPage (subscribeNotifications -> new EventSource(...)). */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return notificationService.subscribe(SecurityUtil.getCurrentUserId());
    }

    /** Returns null when "ids" isn't present as a list, so the caller falls back to mark-all. */
    private List<Long> extractIds(Map<String, Object> body) {
        if (body == null || !(body.get("ids") instanceof List<?> rawIds)) return null;
        return rawIds.stream()
                .map(this::toLong)
                .filter(Objects::nonNull)
                .toList();
    }

    private Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        try {
            return v != null ? Long.parseLong(String.valueOf(v)) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
