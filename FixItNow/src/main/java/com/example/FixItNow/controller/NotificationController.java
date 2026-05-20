package com.example.FixItNow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.response.ApiResponse;
import com.example.FixItNow.entity.Notification;
import com.example.FixItNow.service.NotificationService;

import java.util.List;

/** Notification management endpoints (SRS FR12). */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<Notification>>> getForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved", notificationService.getForUser(userId)));
    }

    @GetMapping("/{userId}/unread")
    public ResponseEntity<ApiResponse<List<Notification>>> getUnread(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok("Unread notifications", notificationService.getUnreadForUser(userId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }
}