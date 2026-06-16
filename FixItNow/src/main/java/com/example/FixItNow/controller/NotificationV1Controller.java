package com.example.FixItNow.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.service.NotificationV1Service;
import com.example.FixItNow.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

/** Current-user notifications (lib/booking.ts): /api/v1/notifications, /api/v1/notifications/mark-read. */
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

    @PostMapping("/mark-read")
    public ResponseEntity<Map<String, Object>> markRead() {
        return ResponseEntity.ok(notificationService.markAllRead(SecurityUtil.getCurrentUserId()));
    }
}
