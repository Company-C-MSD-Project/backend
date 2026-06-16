package com.example.FixItNow.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.entity.RefreshToken;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

/**
 * Issues, verifies, rotates, and revokes opaque refresh tokens backing the
 * /api/v1/auth refresh flow (SRS FR2). Tokens are random UUIDs persisted with
 * an expiry so they can be invalidated on logout.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /** Refresh token lifetime in milliseconds (default 7 days). */
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken issue(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString() + UUID.randomUUID())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    /** Returns the token if present and still active, else throws 401. */
    @Transactional(readOnly = true)
    public RefreshToken verify(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token."));
        if (!stored.isActive()) {
            throw new UnauthorizedException("Refresh token expired or revoked. Please sign in again.");
        }
        return stored;
    }

    /** Rotate: revoke the presented token and issue a new one for the same user. */
    @Transactional
    public RefreshToken rotate(RefreshToken current) {
        current.setRevoked(true);
        refreshTokenRepository.save(current);
        return issue(current.getUser());
    }

    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }
}
