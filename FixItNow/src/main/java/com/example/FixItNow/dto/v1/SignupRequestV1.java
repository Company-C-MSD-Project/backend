package com.example.FixItNow.dto.v1;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** POST /api/v1/auth/signup body. role is "homeowner" | "provider" (defaults to homeowner). */
@Getter @Setter
public class SignupRequestV1 {

    @Email(message = "A valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    /** Optional; derived from the email local-part when omitted. */
    private String username;

    /** "homeowner" | "provider"; ignored if it does not match a self-serve role. */
    private String role;
}
