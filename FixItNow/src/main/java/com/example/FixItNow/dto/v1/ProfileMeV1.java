package com.example.FixItNow.dto.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * Current-user profile shape (services/profile.ts ProfileMe). The frontend reads
 * snake_case with camelCase fallbacks; we emit snake_case. district/bio are not
 * yet stored on the User entity and are returned null.
 */
@Getter
@Builder
public class ProfileMeV1 {
    private final String id;
    private final String email;
    private final String username;

    @JsonProperty("display_name")
    private final String displayName;

    @JsonProperty("avatar_url")
    private final String avatarUrl;

    private final String role;
    private final String phone;
    private final String address;
    private final String district;
    private final String bio;
}
