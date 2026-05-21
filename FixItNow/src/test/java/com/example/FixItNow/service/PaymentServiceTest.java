package com.example.FixItNow.unit;

import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.Payment;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.PaymentStatus;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.PaymentRepository;
import com.example.FixItNow.service.NotificationService;
import com.example.FixItNow.service.PaymentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService — FR10 (payments and invoicing).
 *
 * UT-24: confirmPayment transitions status → COMPLETED and sets transactionRef (invoice info).
 * UT-25: initiatePayment for a non-completed booking is rejected before calling Stripe.
 *
 * Note: Stripe API calls (PaymentIntent.create) are not tested here because they are
 * static SDK calls that require real credentials. The service-layer guards (status checks)
 * are tested without ever reaching the Stripe call.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private PaymentService paymentService;

    private User homeowner;
    private User provider;
    private Booking completedBooking;
    private Booking pendingBooking;

    @BeforeEach
    void setUp() {
        // Inject dummy stripe key so @PostConstruct does not fail during Spring context setup
        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", "sk_test_dummy");

        homeowner = User.builder()
                .id(1L).name("Alice").email("alice@test.com")
                .username("alice").passwordHash("hash")
                .userType(UserType.HOMEOWNER).build();

        provider = User.builder()
                .id(2L).name("Bob Provider").email("bob@test.com")
                .username("bob").passwordHash("hash")
                .userType(UserType.SERVICE_PROVIDER).build();

        completedBooking = Booking.builder()
                .id(50L).homeowner(homeowner).provider(provider)
                .status(BookingStatus.COMPLETED)
                .estimatedCost(new BigDecimal("3500.00"))
                .scheduledDate(LocalDateTime.now().minusDays(1))
                .build();

        pendingBooking = Booking.builder()
                .id(51L).homeowner(homeowner).provider(provider)
                .status(BookingStatus.PENDING)
                .estimatedCost(new BigDecimal("3500.00"))
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .build();
    }

    // UT-24: confirmPayment sets status COMPLETED, stamps paidAt, and sets transactionRef (invoice)
    @Test
    @DisplayName("UT-24 | confirmPayment marks payment COMPLETED and sets transaction reference")
    void ut24_confirmPayment_setsCompletedStatusAndTransactionRef() {
        Payment pendingPayment = Payment.builder()
                .id(1L).booking(completedBooking)
                .amount(new BigDecimal("3500.00"))
                .stripePaymentIntent("pi_test_12345")
                .status(PaymentStatus.PENDING)
                .build();

        when(paymentRepository.findByStripePaymentIntent("pi_test_12345"))
                .thenReturn(Optional.of(pendingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment result = paymentService.confirmPayment("pi_test_12345");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getPaidAt()).isNotNull();
        assertThat(result.getTransactionRef()).startsWith("TXN-");
        // Both homeowner and provider are notified
        verify(notificationService).send(eq(homeowner), eq("PAYMENT_COMPLETED"), anyString());
        verify(notificationService).send(eq(provider), eq("PAYMENT_RECEIVED"), anyString());
    }

    // UT-25: initiatePayment for non-COMPLETED booking is rejected (exception before Stripe call)
    @Test
    @DisplayName("UT-25 | initiatePayment for non-completed booking throws BadRequestException")
    void ut25_initiatePaymentForPendingBooking_throwsBadRequest() {
        when(bookingRepository.findById(51L)).thenReturn(Optional.of(pendingBooking));

        assertThatThrownBy(() -> paymentService.initiatePayment(51L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("completed bookings");

        // Stripe is never reached — no payment record created
        verify(paymentRepository, never()).save(any());
    }
}
