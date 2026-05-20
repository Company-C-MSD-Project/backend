package com.example.FixItNow.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.request.LoginRequest;
import com.example.FixItNow.dto.request.RegisterRequest;
import com.example.FixItNow.dto.response.ApiResponse;
import com.example.FixItNow.dto.response.AuthResponse;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.service.AuthService;

/**
 * Authentication endpoints — public (no JWT required).
 * POST /api/auth/register — FR1
 * POST /api/auth/login    — FR2
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        User created = authService.register(request);
        // Never return the password hash in the response
        created.setPasswordHash(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful. " +
                        (created.getUserType().name().equals("SERVICE_PROVIDER")
                                ? "Please wait for admin verification before accepting jobs."
                                : "You can now log in."), created));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }
}