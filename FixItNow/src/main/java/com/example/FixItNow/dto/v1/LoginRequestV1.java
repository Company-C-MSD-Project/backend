package com.example.FixItNow.dto.v1;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** POST /api/v1/auth/login body. */
@Getter @Setter
public class LoginRequestV1 {

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
