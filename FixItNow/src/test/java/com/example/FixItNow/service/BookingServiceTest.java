package com.example.FixItNow.service;

import com.example.FixItNow.dto.request.BookingRequest;
import com.example.FixItNow.entity.Booking;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService.
 
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository   bookingRepository;
    @Mock private UserRepository      userRepository;
    @Mock private ServiceRepository   serviceRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks private BookingService bookingService;

    private User                                  activeHomeowner;
    private User                                  blacklistedHomeowner;
    private com.example.FixItNow.entity.Service   dummyService;

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

        dummyService = new com.example.FixItNow.entity.Service();
        dummyService.setId(10L);
        dummyService.setName("Plumbing");
    }

    // createBooking() 

    /**
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
                .service(dummyService)
                .status(BookingStatus.PENDING)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(activeHomeowner));
        when(serviceRepository.findById(10L)).thenReturn(Optional.of(dummyService));
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
}
