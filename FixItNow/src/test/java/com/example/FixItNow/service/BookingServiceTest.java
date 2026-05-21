package com.example.FixItNow.service;

import com.example.FixItNow.dto.request.BookingRequest;
import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.Service;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.ServiceRepository;
import com.example.FixItNow.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Unit tests for BookingService — covers FR8 (booking lifecycle).
 * Test cases include:
 *   - UT_15–17: creation guards (blacklist, null serviceId, no-provider happy path)
 *   - UT-18–22: creation, scheduled booking, cancellation, and state-machine guards
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Tests")
class BookingServiceTest {

    @Mock private BookingRepository   bookingRepository;
    @Mock private UserRepository      userRepository;
    @Mock private ServiceRepository   serviceRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private BookingService bookingService;

    // Test fixtures
    private User    activeHomeowner;
    private User    blacklistedHomeowner;
    private User    provider;
    private Service plumbingService;

    @BeforeEach
    void setUp() {
        activeHomeowner = User.builder()
                .id(1L)
                .name("Alice Smith")
                .email("alice@example.com")
                .username("alice123")
                .passwordHash("$2a$hashed")
                .userType(UserType.HOMEOWNER)
                .isActive(true)
                .isBlacklisted(false)
                .build();

        blacklistedHomeowner = User.builder()
                .id(5L)
                .name("Dan Bad")
                .email("dan@example.com")
                .username("dan_bad")
                .passwordHash("$2a$hashed3")
                .userType(UserType.HOMEOWNER)
                .isActive(false)
                .isBlacklisted(true)
                .build();

        provider = User.builder()
                .id(20L)
                .name("Bob Provider")
                .email("bob@test.com")
                .username("bob")
                .passwordHash("hash")
                .userType(UserType.SERVICE_PROVIDER)
                .isVerified(true)
                .isActive(true)
                .build();

        plumbingService = Service.builder()
                .id(10L)
                .name("Pipe Leak Repair")
                .dayPayment(new BigDecimal("5000.00"))
                .isActive(true)
                .build();
    }

    // createBooking()

    /*
     * UT_15 – Valid homeownerId, valid serviceId, future scheduledDate,
     *         and providerId = null should create a PENDING booking.
     */
    @Test
    @DisplayName("UT_15: createBooking() – valid request with providerId=null should return PENDING booking")
    void ut15_createBooking_validRequestNoProvider_shouldReturnPendingBooking() {
        BookingRequest req = new BookingRequest();
        req.setServiceId(10L);
        req.setProviderId(null);
        req.setScheduledDate(LocalDateTime.now().plusDays(3));
        req.setDescription("Leaking tap in kitchen");

        Booking expectedBooking = Booking.builder()
                .id(100L)
                .homeowner(activeHomeowner)
                .service(plumbingService)
                .status(BookingStatus.PENDING)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeHomeowner));
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(plumbingService));
        when(bookingRepository.save(any(Booking.class))).thenReturn(expectedBooking);

        Booking result = bookingService.createBooking(1L, req);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getHomeowner().getId()).isEqualTo(1L);
        verify(bookingRepository).save(any(Booking.class));
        // No provider assigned — no notification should be sent
        verify(notificationService, never()).send(any(), anyString(), anyString());
    }

    /**
     * UT_16 – A blacklisted homeowner attempting to create a booking should
     *         receive a BadRequestException. Nothing should be persisted.
     */
    @Test
    @DisplayName("UT_16: createBooking() – blacklisted homeowner should throw BadRequestException")
    void ut16_createBooking_blacklistedHomeowner_shouldThrow() {
        BookingRequest req = new BookingRequest();
        req.setServiceId(10L);
        req.setScheduledDate(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(5L)).thenReturn(Optional.of(blacklistedHomeowner));

        assertThatThrownBy(() -> bookingService.createBooking(5L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("suspended");

        verify(bookingRepository, never()).save(any());
        verify(notificationService, never()).send(any(), anyString(), anyString());
    }

    /**
     * UT_17 – A null serviceId (required field) should cause an exception
     *         before any booking is persisted.
     *
     * Spring JPA throws IllegalArgumentException when findById receives null.
     */
    @Test
    @DisplayName("UT_17: createBooking() – null serviceId should throw and not persist booking")
    void ut17_createBooking_nullServiceId_shouldThrow() {
        BookingRequest req = new BookingRequest();
        req.setServiceId(null);   // required field intentionally left null
        req.setScheduledDate(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeHomeowner));
        when(serviceRepository.findById(null))
                .thenThrow(new IllegalArgumentException("Id must not be null"));

        assertThatThrownBy(() -> bookingService.createBooking(1L, req))
                .isInstanceOf(Exception.class);

        verify(bookingRepository, never()).save(any());
    }

    /** UT-18 – Create instant booking (no specific provider) 
     * status is PENDING,
     * estimatedCost is derived from the service's dayPayment rate.
     */
    @Test
    @DisplayName("UT-18 | createBooking without provider creates PENDING booking")
    void ut18_createInstantBooking_statusIsPending() {
        BookingRequest req = new BookingRequest();
        req.setServiceId(10L);
        req.setScheduledDate(LocalDateTime.now().plusDays(1));
        req.setDescription("Kitchen pipe is leaking");
        req.setServiceType("PLUMBING");

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeHomeowner));
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(plumbingService));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });

        Booking result = bookingService.createBooking(1L, req);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(result.getEstimatedCost()).isEqualByComparingTo("5000.00");
        assertThat(result.getHomeowner().getId()).isEqualTo(1L);
        // No provider assigned — no notification should be sent
        verify(notificationService, never()).send(any(), anyString(), anyString());
    }

    /** UT-19 – Create scheduled booking with a specific provider and future date.
     *  scheduledDate is preserved and provider is notified.
     */
    @Test
    @DisplayName("UT-19 | createBooking with future scheduledDate persists the date correctly")
    void ut19_createScheduledBooking_scheduledDateIsPreserved() {
        LocalDateTime nextWeek = LocalDateTime.now().plusDays(7);

        BookingRequest req = new BookingRequest();
        req.setServiceId(10L);
        req.setScheduledDate(nextWeek);
        req.setProviderId(20L);
        req.setDescription("Kitchen pipe is leaking");
        req.setServiceType("PLUMBING");

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeHomeowner));
        when(userRepository.findById(20L)).thenReturn(Optional.of(provider));
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(plumbingService));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(101L);
            return b;
        });

        Booking result = bookingService.createBooking(1L, req);

        assertThat(result.getScheduledDate()).isEqualTo(nextWeek);
        assertThat(result.getProvider().getId()).isEqualTo(20L);
        // Provider notified of the new job request
        verify(notificationService).send(eq(provider), eq("JOB_REQUEST"), anyString());
    }

    // cancelBooking()

    /** UT-20 – Cancel a PENDING booking.
     *   status becomes CANCELLED with cancellation reason stored.*/

    @Test
    @DisplayName("UT-20 | cancelBooking on PENDING booking sets status to CANCELLED")
    void ut20_cancelPendingBooking_statusIsCancelled() {
        Booking pending = Booking.builder()
                .id(100L)
                .homeowner(activeHomeowner)
                .provider(provider)
                .service(plumbingService)
                .status(BookingStatus.PENDING)
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .build();

        when(bookingRepository.findById(100L)).thenReturn(Optional.of(pending));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBooking(100L, "Provider unavailable");

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(result.getCancellationReason()).isEqualTo("Provider unavailable");
    }

    /** UT-21 – Cannot cancel a COMPLETED booking.
     *  BadRequestException thrown, booking is never re-persisted. */
     
    @Test
    @DisplayName("UT-21 | cancelBooking on COMPLETED booking throws BadRequestException")
    void ut21_cancelCompletedBooking_throwsBadRequest() {
        Booking completed = Booking.builder()
                .id(101L)
                .homeowner(activeHomeowner)
                .provider(provider)
                .service(plumbingService)
                .status(BookingStatus.COMPLETED)
                .scheduledDate(LocalDateTime.now().minusDays(1))
                .build();

        when(bookingRepository.findById(101L)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> bookingService.cancelBooking(101L, "Change of mind"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot cancel");

        verify(bookingRepository, never()).save(any());
    }
}
