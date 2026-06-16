package com.example.FixItNow.dto.v1;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** POST /api/v1/auth/refresh body: { "refresh_token": "..." }. */
@Getter @Setter
public class RefreshRequestV1 {

    @JsonProperty("refresh_token")
    @NotBlank(message = "refresh_token is required")
    private String refreshToken;
}
