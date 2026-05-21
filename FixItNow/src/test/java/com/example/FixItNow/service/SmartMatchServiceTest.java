package com.example.FixItNow.unit;

import com.example.FixItNow.dto.response.ProviderMatchResponse;
import com.example.FixItNow.entity.ProviderLocation;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.repository.ProviderLocationRepository;
import com.example.FixItNow.repository.UserRepository;
import com.example.FixItNow.service.SmartMatchService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SmartMatchService — FR7 (search and filter providers).
 * Validates category filtering, rating-based ranking, and graceful empty results.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SmartMatchService Unit Tests")
class SmartMatchServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ProviderLocationRepository locationRepository;
    @InjectMocks private SmartMatchService smartMatchService;

    // Homeowner coordinates (Colombo city centre)
    private static final double HOME_LAT = 6.9271;
    private static final double HOME_LNG = 79.8612;

    private User makeProvider(Long id, String category, double rating, boolean available) {
        return User.builder()
                .id(id).name("Provider " + id).email("p" + id + "@test.com")
                .username("prov" + id).passwordHash("hash")
                .userType(UserType.SERVICE_PROVIDER)
                .serviceCategory(category)
                .rating(rating)
                .isVerified(true).isActive(available)
                .build();
    }

    private ProviderLocation nearLocation(Long providerId) {
        // ~2 km from homeowner
        return ProviderLocation.builder()
                .id(providerId)
                .latitude(6.9400)
                .longitude(79.8500)
                .build();
    }

    // UT-16: Filter by service type (category) — only plumbers returned for "PLUMBING"
    @Test
    @DisplayName("UT-16 | findMatches queries only the requested service category")
    void ut16_filterByCategory_onlyMatchingCategoryReturned() {
        User plumber = makeProvider(1L, "PLUMBING", 4.2, true);
        when(userRepository.findVerifiedProvidersByCategory("PLUMBING"))
                .thenReturn(List.of(plumber));
        when(locationRepository.findByProviderId(1L))
                .thenReturn(Optional.of(nearLocation(1L)));

        List<ProviderMatchResponse> results = smartMatchService.findMatches("PLUMBING", HOME_LAT, HOME_LNG);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getServiceCategory()).isEqualTo("PLUMBING");
        verify(userRepository).findVerifiedProvidersByCategory("PLUMBING");
    }

    // UT-17: Higher-rated provider is ranked first when distances are equal
    @Test
    @DisplayName("UT-17 | Provider with higher rating is ranked above lower-rated one at equal distance")
    void ut17_filterByMinRating_higherRatedProviderRankedFirst() {
        User lowRated  = makeProvider(1L, "ELECTRICAL", 2.5, true);
        User highRated = makeProvider(2L, "ELECTRICAL", 4.8, true);

        when(userRepository.findVerifiedProvidersByCategory("ELECTRICAL"))
                .thenReturn(List.of(lowRated, highRated));

        // Same location for both → distance does not affect relative ranking
        when(locationRepository.findByProviderId(1L)).thenReturn(Optional.of(nearLocation(1L)));
        when(locationRepository.findByProviderId(2L)).thenReturn(Optional.of(nearLocation(2L)));

        List<ProviderMatchResponse> results = smartMatchService.findMatches("ELECTRICAL", HOME_LAT, HOME_LNG);

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results.get(0).getRating()).isGreaterThan(results.get(1).getRating());
    }

    // UT-18: findVerifiedProvidersByCategory already enforces isActive=true (availability flag)
    @Test
    @DisplayName("UT-18 | Only active (available) providers are returned by the repository query")
    void ut18_availabilityFlag_inactiveProviderExcludedByRepository() {
        // The repository query enforces isActive=true — simulate by returning only one active provider
        User activeProvider = makeProvider(1L, "CARPENTRY", 4.0, true);
        // inactive provider is NOT returned by findVerifiedProvidersByCategory (filtered at DB layer)
        when(userRepository.findVerifiedProvidersByCategory("CARPENTRY"))
                .thenReturn(List.of(activeProvider));
        when(locationRepository.findByProviderId(1L)).thenReturn(Optional.of(nearLocation(1L)));

        List<ProviderMatchResponse> results = smartMatchService.findMatches("CARPENTRY", HOME_LAT, HOME_LNG);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProviderId()).isEqualTo(1L);
    }

    // UT-19: Empty result set handled gracefully — no exception, returns empty list
    @Test
    @DisplayName("UT-19 | No matching providers returns an empty list without exception")
    void ut19_noMatchingProviders_returnsEmptyListGracefully() {
        when(userRepository.findVerifiedProvidersByCategory("ROOFING"))
                .thenReturn(List.of());

        List<ProviderMatchResponse> results = smartMatchService.findMatches("ROOFING", HOME_LAT, HOME_LNG);

        assertThat(results).isEmpty();
    }

    // Extra: Haversine distance calculation is accurate to within 0.5 km
    @Test
    @DisplayName("Haversine formula computes known distance correctly")
    void haversine_knownCoordinates_correctDistanceKm() {
        // Colombo Fort (6.9333, 79.8500) to Dehiwala (~8 km south, 6.8500, 79.8650)
        double dist = SmartMatchService.haversineKm(6.9333, 79.8500, 6.8500, 79.8650);
        assertThat(dist).isBetween(9.0, 11.0);
    }
}
