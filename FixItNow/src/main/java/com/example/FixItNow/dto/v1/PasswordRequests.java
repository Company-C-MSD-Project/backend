package com.example.FixItNow.dto.v1;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Request bodies for the password endpoints. */
public class PasswordRequests {

    /** POST /api/v1/auth/password/forgot body: { "email": "..." }. */
    @Getter @Setter
    public static class ForgotPassword {
        @Email(message = "A valid email is required")
        @NotBlank(message = "Email is required")
        private String email;
    }

    /** POST /api/v1/auth/password/reset body: { "password": "..." }. */
    @Getter @Setter
    public static class ResetPassword {
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }
}
