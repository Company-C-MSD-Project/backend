package com.example.FixItNow.dto.response;

import com.example.FixItNow.enums.UserType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/** Response returned on successful login — includes JWT token and user role. */
@Data @Builder @AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String email;
    private UserType userType;
}
