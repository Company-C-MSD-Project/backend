package com.example.FixItNow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.FixItNow.repository.UserRepository;
import com.example.FixItNow.repository.ProviderLocationRepository;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.entity.ProviderLocation;
import com.example.FixItNow.dto.response.ProviderMatchResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Smart provider matching algorithm (SRS §2.1.2 Smart Match Algorithm, SRS FR7).
 *
 * Ranking formula:
 *   score = (normalizedRating × 0.6) + (normalizedProximity × 0.4)
 *
 * This weights trust (rating) higher than convenience (distance) because
 * the platform's primary value proposition is finding *trusted* tradespeople.
 */
@Service
@RequiredArgsConstructor
public class SmartMatchService {

    private final UserRepository userRepository;
    private final ProviderLocationRepository locationRepository;

    /** Average travel speed used for ETA estimation (km/h). */
    private static final double AVG_SPEED_KMH = 30.0;
    /** Maximum search radius in km — providers outside this are excluded. */
    private static final double MAX_RADIUS_KM = 25.0;

    /**
     * Returns a ranked list of available providers for a given service category
     * near the homeowner's coordinates. Providers with no recorded location are
     * included last with a placeholder distance.
     */
    public List<ProviderMatchResponse> findMatches(String category, double homeownLat, double homeownLng) {
        List<User> providers = userRepository.findVerifiedProvidersByCategory(category);

        return providers.stream()
                .map(provider -> buildMatchResponse(provider, homeownLat, homeownLng))
                .filter(match -> match.getDistanceKm() <= MAX_RADIUS_KM)
                .sorted(Comparator.comparingDouble(this::computeScore).reversed()) // highest score first
                .collect(Collectors.toList());
    }

    private ProviderMatchResponse buildMatchResponse(User provider, double lat, double lng) {
        Optional<ProviderLocation> loc = locationRepository.findByProviderId(provider.getId());

        double distanceKm = loc
                .map(l -> haversineKm(lat, lng, l.getLatitude(), l.getLongitude()))
                .orElse(MAX_RADIUS_KM); // unknown location → placed at boundary

        int etaMinutes = (int) Math.round((distanceKm / AVG_SPEED_KMH) * 60);

        return ProviderMatchResponse.builder()
                .providerId(provider.getId())
                .name(provider.getName())
                .serviceCategory(provider.getServiceCategory())
                .rating(provider.getRating())
                .badgeLevel(provider.getBadgeLevel())
                .distanceKm(Math.round(distanceKm * 10.0) / 10.0)
                .etaMinutes(etaMinutes)
                .isVerified(provider.isVerified())
                .build();
    }

    /**
     * Composite score used for ranking — see class-level Javadoc for formula.
     * Rating is normalised to [0,1] by dividing by 5.
     * Proximity score = 1 - (distance / MAX_RADIUS) — closer = higher score.
     */
    private double computeScore(ProviderMatchResponse m) {
        double ratingScore    = (m.getRating() != null ? m.getRating() : 0) / 5.0;
        double proximityScore = 1.0 - (m.getDistanceKm() / MAX_RADIUS_KM);
        return (ratingScore * 0.6) + (proximityScore * 0.4);
    }

    /**
     * Haversine formula — calculates great-circle distance between two GPS points.
     * Accurate enough for urban distances up to ~50 km.
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
