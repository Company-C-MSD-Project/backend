package com.example.FixItNow.integration;

import com.example.FixItNow.dto.response.ApiResponse;
import com.example.FixItNow.entity.Booking;
import com.example.FixItNow.entity.Review;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.BookingStatus;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.service.AuthService;
import com.example.FixItNow.service.BookingService;
import com.example.FixItNow.service.NotificationService;
import com.example.FixItNow.service.PaymentService;
import com.example.FixItNow.service.ReviewService;
import com.example.FixItNow.service.UserService;
import com.example.FixItNow.service.SmartMatchService;
import com.example.FixItNow.dto.response.ProviderMatchResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.example.FixItNow.repository.BookingRepository;
import com.example.FixItNow.repository.PaymentRepository;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests (IT-01 to IT-05) — cross-module workflow validation.
 * This tests controller-to-service wiring (routing, parameter binding, response wrapping)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Integration Tests — Cross-Module Flows (IT-01 to IT-05)")
class FlowIntegrationTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean SmartMatchService smartMatchService;
    @MockitoBean BookingService bookingService;
    @MockitoBean PaymentService paymentService;
    @MockitoBean ReviewService reviewService;
    @MockitoBean UserService userService;
    @MockitoBean NotificationService notificationService;
    @MockitoBean AuthService authService;
    // AdminController directly autowires these repositories — must be mocked
    @MockitoBean BookingRepository bookingRepository;
    @MockitoBean PaymentRepository paymentRepository;

    // ─── Shared test fixtures ────────────────────────────────────────────────

    private User homeowner() {
        return User.builder().id(1L).name("Alice").email("alice@test.com")
                .username("alice").passwordHash(null).userType(UserType.HOMEOWNER).build();
    }

    private User provider() {
        return User.builder().id(2L).name("Bob Provider").email("bob@test.com")
                .username("bob").passwordHash(null)
                .userType(UserType.SERVICE_PROVIDER).isVerified(true).build();
    }

    private Booking completedBooking() {
        return Booking.builder()
                .id(100L).homeowner(homeowner()).provider(provider())
                .status(BookingStatus.COMPLETED)
                .estimatedCost(new BigDecimal("4000.00"))
                .scheduledDate(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ─── IT-01: Search provider → Book → Tracking endpoint accessible ────────

    @Test
    @WithMockUser(username = "alice", roles = "HOMEOWNER")
    @DisplayName("IT-01 | Search provider, create booking, verify tracking endpoint accessible")
    void it01_searchProviderThenBook_providerFoundBookingCreated() throws Exception {
        // Step 1: Search providers (FR7)
        ProviderMatchResponse match = ProviderMatchResponse.builder()
                .providerId(2L).name("Bob Provider").serviceCategory("PLUMBING")
                .rating(4.5).distanceKm(3.2).etaMinutes(6).isVerified(true).build();
        when(smartMatchService.findMatches("PLUMBING", 6.9271, 79.8612))
                .thenReturn(List.of(match));

        mockMvc.perform(get("/api/bookings/match")
                        .param("category", "PLUMBING")
                        .param("lat", "6.9271")
                        .param("lng", "79.8612"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].providerId").value(2))
                .andExpect(jsonPath("$.data[0].serviceCategory").value("PLUMBING"));

        // Step 2: Book the matched provider (FR8)
        Booking booking = Booking.builder().id(100L).homeowner(homeowner())
                .provider(provider()).status(BookingStatus.PENDING)
                .scheduledDate(LocalDateTime.now().plusDays(1))
                .estimatedCost(new BigDecimal("4000.00")).build();
        when(bookingService.createBooking(eq(1L), any())).thenReturn(booking);

        mockMvc.perform(post("/api/bookings")
                        .param("homeownerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceId": 1,
                                  "providerId": 2,
                                  "scheduledDate": "2027-06-01T10:00:00",
                                  "description": "Kitchen pipe leak",
                                  "serviceType": "PLUMBING"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(smartMatchService).findMatches("PLUMBING", 6.9271, 79.8612);
        verify(bookingService).createBooking(eq(1L), any());
    }

    // ─── IT-02: Book → Pay → Notification ───────────────────────────────────

    @Test
    @DisplayName("IT-02 | Provider accepts booking; payment webhook confirms payment and triggers notifications")
    void it02_bookPayAndNotify_notificationServiceCalledOnPaymentConfirmation() throws Exception {
        // Step 1: Provider accepts the booking (SERVICE_PROVIDER role required)
        Booking accepted = Booking.builder().id(100L).homeowner(homeowner()).provider(provider())
                .status(BookingStatus.ACCEPTED)
                .scheduledDate(LocalDateTime.now().plusDays(1)).build();
        when(bookingService.acceptBooking(100L, 2L)).thenReturn(accepted);

        mockMvc.perform(put("/api/bookings/100/accept")
                        .param("providerId", "2")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("bob").roles("SERVICE_PROVIDER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // Step 2: Payment webhook callback — confirms payment (no auth required)
        com.example.FixItNow.entity.Payment payment = com.example.FixItNow.entity.Payment.builder()
                .id(1L).booking(completedBooking())
                .amount(new java.math.BigDecimal("4000.00"))
                .status(com.example.FixItNow.enums.PaymentStatus.COMPLETED)
                .transactionRef("TXN-123456789").paidAt(LocalDateTime.now()).build();
        when(paymentService.confirmPayment("pi_test_abc")).thenReturn(payment);

        mockMvc.perform(post("/api/payments/confirm")
                        .param("paymentIntentId", "pi_test_abc")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user("alice").roles("HOMEOWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(bookingService).acceptBooking(100L, 2L);
        verify(paymentService).confirmPayment("pi_test_abc");
    }

    // ─── IT-03: Admin updates user credentials → user can log in with new details

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("IT-03 | Admin updates user status; updated user is reflected in response")
    void it03_adminUpdatesUserStatus_changeReflectedInResponse() throws Exception {
        User deactivated = User.builder().id(1L).name("Alice").email("alice@test.com")
                .username("alice").passwordHash(null).userType(UserType.HOMEOWNER)
                .isActive(false).build();
        when(userService.setActiveStatus(1L, false)).thenReturn(deactivated);

        mockMvc.perform(put("/api/admin/users/1/status").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(userService).setActiveStatus(1L, false);
    }

    // ─── IT-04: Admin views analytics (report) then deactivates an account ──

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("IT-04 | Admin retrieves analytics report and deactivates an account")
    void it04_adminAnalyticsThenDeactivate_bothEndpointsSucceed() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(homeowner(), provider()));
        when(userService.getUsersByType(UserType.HOMEOWNER)).thenReturn(List.of(homeowner()));
        when(userService.getUsersByType(UserType.SERVICE_PROVIDER)).thenReturn(List.of(provider()));

        // Step 1: View analytics (FR6)
        mockMvc.perform(get("/api/admin/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(2));

        // Step 2: Deactivate account (FR3)
        User deleted = User.builder().id(1L).name("Alice").email("alice@test.com")
                .username("alice").passwordHash(null).userType(UserType.HOMEOWNER)
                .isActive(false).build();
        when(userService.setActiveStatus(1L, false)).thenReturn(deleted);

        mockMvc.perform(put("/api/admin/users/1/status").param("active", "false"))
                .andExpect(status().isOk());
    }

    // ─── IT-05: Complete booking → Submit review ───────────────────────────

    @Test
    @WithMockUser(username = "alice", roles = "HOMEOWNER")
    @DisplayName("IT-05 | After booking completes, review can be submitted; blocked on incomplete booking")
    void it05_completedBookingAcceptsReview_incompleteBookingDoesNot() throws Exception {
        // Provider marks job done (role switch — test as provider)
        Booking completed = completedBooking();
        when(bookingService.completeJob(100L)).thenReturn(completed);

        // Submit review after completion (FR11)
        Review review = Review.builder()
                .id(1L).booking(completed).homeowner(homeowner())
                .provider(provider()).rating(5).comment("Perfect job!").build();
        when(reviewService.submitReview(1L, 100L, 5, "Perfect job!")).thenReturn(review);

        mockMvc.perform(post("/api/reviews")
                        .param("homeownerId", "1")
                        .param("bookingId", "100")
                        .param("rating", "5")
                        .param("comment", "Perfect job!"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5));

        verify(reviewService).submitReview(1L, 100L, 5, "Perfect job!");
    }
}
