package com.example.FixItNow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real-time GPS tracking for service providers (SRS FR9).
 * Provider pushes location updates → stored in DB → broadcast to homeowner
 * via WebSocket topic /topic/tracking/{bookingId}.
 */
@Service
@RequiredArgsConstructor
public class TrackingService {

    private final ProviderLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Called by provider client on each GPS update.
     * Upsert location row and broadcast to subscribed homeowner WebSocket clients.
     */
    @Transactional
    public ProviderLocation updateLocation(Long providerId, double lat, double lng, Long bookingId) {
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found"));

        ProviderLocation location = locationRepository.findByProviderId(providerId)
                .orElse(ProviderLocation.builder().provider(provider).build());

        location.setLatitude(lat);
        location.setLongitude(lng);
        ProviderLocation saved = locationRepository.save(location);

        // Push update over WebSocket to homeowners tracking this booking
        if (bookingId != null) {
            messagingTemplate.convertAndSend(
                    "/topic/tracking/" + bookingId,
                    new LocationPayload(providerId, lat, lng));
        }

        return saved;
    }

    public ProviderLocation getProviderLocation(Long providerId) {
        return locationRepository.findByProviderId(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not available for provider: " + providerId));
    }

    /** Lightweight payload broadcast over WebSocket — avoids sending full entity. */
    public record LocationPayload(Long providerId, double lat, double lng) {}
}
