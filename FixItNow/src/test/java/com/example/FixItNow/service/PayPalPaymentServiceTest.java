package com.example.FixItNow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.Payment;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.entity.WalletTransaction;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.PaymentStatus;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.PaymentRepository;
import com.example.FixItNow.repository.WalletTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PayPalPaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private NotificationService notificationService;
    @Mock private PayPalClient payPalClient;

    @InjectMocks private PayPalPaymentService service;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Booking booking;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "currency", "USD");
        User homeowner = User.builder()
                .id(1L).name("Homeowner").email("home@test.com").username("home")
                .passwordHash("hash").userType(UserType.HOMEOWNER).build();
        User provider = User.builder()
                .id(2L).name("Provider").email("provider@test.com").username("provider")
                .passwordHash("hash").userType(UserType.SERVICE_PROVIDER).build();
        booking = Booking.builder()
                .id(50L).homeowner(homeowner).provider(provider)
                .status(BookingStatus.COMPLETED)
                .estimatedCost(new BigDecimal("25.00"))
                .scheduledDate(LocalDateTime.now())
                .build();
    }

    @Test
    void createOrderPersistsPendingPayPalPayment() throws Exception {
        when(bookingRepository.findById(50L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(50L)).thenReturn(Optional.empty());
        when(payPalClient.createOrder(50L, new BigDecimal("25.00"), "USD"))
                .thenReturn(objectMapper.readTree("""
                        {"id":"ORDER-123","status":"CREATED",
                         "links":[{"rel":"approve","href":"https://paypal.test/approve"}]}
                        """));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createOrder(50L);

        assertThat(response.orderId()).isEqualTo("ORDER-123");
        assertThat(response.approveUrl()).isEqualTo("https://paypal.test/approve");
        assertThat(response.payment().getMethod()).isEqualTo("PAYPAL");
        assertThat(response.payment().getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void captureIsIdempotentAndCreditsWalletOnce() throws Exception {
        Payment payment = Payment.builder()
                .id(10L).booking(booking).amount(new BigDecimal("25.00"))
                .method("PAYPAL").paypalOrderId("ORDER-123")
                .status(PaymentStatus.PENDING).build();
        when(paymentRepository.findByPaypalOrderId("ORDER-123")).thenReturn(Optional.of(payment));
        when(payPalClient.captureOrder("ORDER-123")).thenReturn(objectMapper.readTree("""
                {"status":"COMPLETED","purchase_units":[{"payments":{"captures":[{"id":"CAP-123"}]}}]}
                """));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletTransactionRepository.existsByReference("PAYPAL-ORDER-123")).thenReturn(false);

        Payment first = service.captureOrder("ORDER-123");
        Payment retry = service.captureOrder("ORDER-123");

        assertThat(first.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(first.getPaypalCaptureId()).isEqualTo("CAP-123");
        assertThat(retry).isSameAs(first);
        verify(payPalClient, times(1)).captureOrder("ORDER-123");
        verify(walletTransactionRepository, times(1)).save(any(WalletTransaction.class));
        verify(notificationService, times(2)).send(any(), any(), any());
    }

    @Test
    void completedCaptureDoesNotCreateAnotherWalletCredit() {
        Payment completed = Payment.builder()
                .booking(booking).amount(new BigDecimal("25.00"))
                .paypalOrderId("ORDER-123").status(PaymentStatus.COMPLETED).build();
        when(paymentRepository.findByPaypalOrderId("ORDER-123")).thenReturn(Optional.of(completed));

        assertThat(service.captureOrder("ORDER-123")).isSameAs(completed);

        verify(payPalClient, never()).captureOrder(any());
        verify(walletTransactionRepository, never()).save(any());
    }
}
