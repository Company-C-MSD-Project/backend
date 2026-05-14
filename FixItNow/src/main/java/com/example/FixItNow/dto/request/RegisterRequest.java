package com.example.FixItNow.dto.request;

import com.example.FixItNow.enums.UserType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body for user registration (SRS FR1). */
@Data
public class RegisterRequest {

    @NotBlank
    private String name;

    @Email @NotBlank
    private String email;

    @NotBlank @Size(min = 3, max = 50)
    private String username;

    /** Minimum 8 characters required for password security. */
    @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String phone;
    private String address;

    @NotNull
    private UserType userType;

    // ServiceProvider-specific (optional for homeowners)
    private String serviceCategory;
    private String department;  // admin only
}
