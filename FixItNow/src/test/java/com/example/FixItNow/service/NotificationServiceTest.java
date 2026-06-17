package com.example.FixItNow.unit;

import com.example.FixItNow.entity.Notification;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.NotificationChannel;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.repository.NotificationRepository;
import com.example.FixItNow.service.NotificationService;
import com.example.FixItNow.service.NotificationV1Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService — FR12 (notifications).
 * Tests that booking confirmation and payment receipt notifications
 * are persisted with the correct type and channel.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private JavaMailSender mailSender;
    @Mock private NotificationV1Service notificationV1Service;

    @InjectMocks private NotificationService notificationService;

    private User recipient;

    @BeforeEach
    void setUp() {
        recipient = User.builder()
                .id(1L).name("Alice").email("alice@test.com")
                .username("alice").passwordHash("hash")
                .userType(UserType.HOMEOWNER).build();
    }

    // UT-29: Build booking confirmation notification → persisted with correct type and BOTH channel
    @Test
    @DisplayName("UT-29 | Booking confirmation notification is persisted with BOTH channel")
    void ut29_bookingConfirmationNotification_persistedWithCorrectTypeAndChannel() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(captor.capture()))
                .thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(1L);
                    return n;
                });

        Notification result = notificationService.send(
                recipient, "BOOKING_ACCEPTED", "Your booking #100 has been accepted.");

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("BOOKING_ACCEPTED");
        assertThat(saved.getMessage()).contains("booking #100");
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.BOTH);
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getUser()).isEqualTo(recipient);
    }

    // UT-30: Build payment receipt notification → persisted with PAYMENT_COMPLETED type
    @Test
    @DisplayName("UT-30 | Payment receipt notification is persisted with PAYMENT_COMPLETED type")
    void ut30_paymentReceiptNotification_persistedWithPaymentCompletedType() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(captor.capture()))
                .thenAnswer(inv -> {
                    Notification n = inv.getArgument(0);
                    n.setId(2L);
                    return n;
                });

        Notification result = notificationService.send(
                recipient, "PAYMENT_COMPLETED", "Payment of $3500.00 confirmed for booking #50.");

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(saved.getMessage()).contains("$3500.00");
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.BOTH);
        verify(notificationRepository).save(any(Notification.class));
    }

    // Extra: sendEmail catches exceptions and does not propagate (best-effort)
    @Test
    @DisplayName("Email send failure is swallowed — in-app notification still returns successfully")
    void sendEmail_onMailFailure_doesNotPropagateException() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatNoException().isThrownBy(() ->
                notificationService.send(recipient, "JOB_STARTED", "Provider has arrived."));
    }
}
