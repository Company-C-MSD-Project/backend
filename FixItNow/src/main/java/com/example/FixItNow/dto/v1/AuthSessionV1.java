package com.example.FixItNow.dto.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * Session shape consumed by the frontend (lib/auth-api.ts AuthSession).
 * Serializes as: access_token, refresh_token, user. Returned raw (not wrapped
 * in ApiResponse) because the new-API client reads the body directly.
 */
@Getter
@Builder
public class AuthSessionV1 {

    @JsonProperty("access_token")
    private final String accessToken;

    @JsonProperty("refresh_token")
    private final String refreshToken;

    private final AuthUserV1 user;
}
