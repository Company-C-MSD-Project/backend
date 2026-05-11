package com.example.FixItNow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for login — accepts email or username (SRS FR2). */
@Data
public class LoginRequest {

    @NotBlank
    private String usernameOrEmail;

    @NotBlank
    private String password;
}
