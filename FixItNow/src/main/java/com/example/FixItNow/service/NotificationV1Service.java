package com.example.FixItNow.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.entity.Notification;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

/** Current-user notifications for the /api/v1/notifications contract (lib/booking.ts). */
@Service
@RequiredArgsConstructor
public class NotificationV1Service {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long userId, boolean unreadOnly) {
        requireUser(userId);
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalse(userId)
                : notificationRepository.findByUserIdOrderBySentAtDesc(userId);
        return notifications.stream().map(this::toMap).toList();
    }

    @Transactional
    public Map<String, Object> markAllRead(Long userId) {
        requireUser(userId);
        List<Notification> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return Map.of("ok", true, "marked", unread.size());
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(n.getId()));
        m.put("type", n.getType());
        m.put("message", n.getMessage());
        m.put("read", n.isRead());
        m.put("created_at", n.getSentAt() != null ? n.getSentAt().toString() : null);
        return m;
    }

    private void requireUser(Long userId) {
        if (userId == null) throw new UnauthorizedException("Not authenticated.");
    }
}
