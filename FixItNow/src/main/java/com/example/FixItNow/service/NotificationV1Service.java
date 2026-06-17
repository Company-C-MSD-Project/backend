package com.example.FixItNow.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.FixItNow.entity.Notification;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Current-user notifications for the /api/v1/notifications contract (lib/booking.ts). */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationV1Service {

    private final NotificationRepository notificationRepository;

    /** Open SSE connections per user, for /api/v1/notifications/stream (NotificationsPage). */
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

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

    /** Marks only the given notification ids read, ignoring any that don't belong to this user. */
    @Transactional
    public Map<String, Object> markRead(Long userId, List<Long> ids) {
        requireUser(userId);
        if (ids == null || ids.isEmpty()) return Map.of("ok", true, "marked", 0);
        List<Notification> mine = notificationRepository.findAllById(ids).stream()
                .filter(n -> n.getUser() != null && userId.equals(n.getUser().getId()))
                .filter(n -> !n.isRead())
                .toList();
        mine.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(mine);
        return Map.of("ok", true, "marked", mine.size());
    }

    /**
     * Registers a new SSE connection for the user's live notification feed
     * (frontend/src/lib/notifications-data.ts subscribeNotifications). No timeout is set on the
     * emitter itself — the browser's EventSource reconnects transparently if anything upstream
     * (proxy/load balancer) closes the connection, so periodic reconnects are expected and fine.
     */
    public SseEmitter subscribe(Long userId) {
        requireUser(userId);
        SseEmitter emitter = new SseEmitter(0L);
        List<SseEmitter> userEmitters = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);

        emitter.onCompletion(() -> userEmitters.remove(emitter));
        emitter.onTimeout(() -> {
            userEmitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(ex -> userEmitters.remove(emitter));

        try {
            // Initial event so the client knows the stream is live (and so proxies flush headers).
            emitter.send(SseEmitter.event().name("ready").data(Map.of("ok", true)));
        } catch (IOException ex) {
            userEmitters.remove(emitter);
        }
        return emitter;
    }

    /** Pushes a newly created notification to any open SSE connections for its recipient. */
    public void push(Long userId, Notification notification) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null || userEmitters.isEmpty()) return;
        Map<String, Object> payload = toMap(notification);
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (IOException | IllegalStateException ex) {
                log.debug("Dropping dead SSE emitter for user {}: {}", userId, ex.getMessage());
                userEmitters.remove(emitter);
            }
        }
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
