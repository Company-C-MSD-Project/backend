package com.example.FixItNow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.repository.NotificationRepository;
import com.example.FixItNow.entity.Notification;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.NotificationChannel;
import com.example.FixItNow.exception.ResourceNotFoundException;

import java.util.List;

/**
 * Notification dispatch for in-app and email channels (SRS FR12).
 * Email sending is @Async to avoid blocking the calling request thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;
    private final NotificationV1Service notificationV1Service;

    /** Persist in-app notification, push it to any live SSE stream, and asynchronously email it. */
    @Transactional
    public Notification send(User user, String type, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .channel(NotificationChannel.BOTH)
                .isRead(false)
                .build();
        Notification saved = notificationRepository.save(notification);
        notificationV1Service.push(user.getId(), saved);
        sendEmail(user.getEmail(), type, message);
        return saved;
    }

    @Async
    public void sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(toEmail);
            mail.setSubject("[FixItNow] " + subject);
            mail.setText(body);
            mailSender.send(mail);
        } catch (Exception e) {
            // Log the failure but don't propagate — email is best-effort,
            // the in-app notification is already saved.
            log.warn("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    public List<Notification> getForUser(Long userId) {
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId);
    }

    public List<Notification> getUnreadForUser(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}