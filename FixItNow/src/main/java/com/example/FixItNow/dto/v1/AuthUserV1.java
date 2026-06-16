package com.example.FixItNow.dto.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * User shape consumed by the frontend auth layer (lib/auth-api.ts AuthUser).
 * Serializes as: id, email, username, display_name, avatar_url, role.
 * role is the lowercased UI role: "homeowner" | "provider" | "admin".
 */
@Getter
@Builder
public class AuthUserV1 {
    private final String id;
    private final String email;
    private final String username;

    @JsonProperty("display_name")
    private final String displayName;

    @JsonProperty("avatar_url")
    private final String avatarUrl;

    private final String role;
}
