package com.example.FixItNow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.FixItNow.dto.v1.AuthSessionV1;
import com.example.FixItNow.dto.v1.AuthUserV1;
import com.example.FixItNow.dto.v1.LoginRequestV1;
import com.example.FixItNow.dto.v1.PasswordRequests;
import com.example.FixItNow.dto.v1.RefreshRequestV1;
import com.example.FixItNow.dto.v1.SignupRequestV1;
import com.example.FixItNow.service.AuthV1Service;
import com.example.FixItNow.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Auth endpoints matching the frontend contract (lib/auth-api.ts). Responses are
 * returned as raw JSON (snake_case) rather than wrapped in ApiResponse because the
 * new-API client reads the body directly. Public endpoints are permitted in
 * SecurityConfig; logout/session/password-reset require a valid access token.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthV1Controller {

    private final AuthV1Service authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthSessionV1> signup(@Valid @RequestBody SignupRequestV1 req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthSessionV1> login(@Valid @RequestBody LoginRequestV1 req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthSessionV1> refresh(@Valid @RequestBody RefreshRequestV1 req) {
        return ResponseEntity.ok(authService.refresh(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout(SecurityUtil.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/session")
    public ResponseEntity<AuthUserV1> session() {
        return ResponseEntity.ok(authService.session(SecurityUtil.getCurrentUserId()));
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody PasswordRequests.ForgotPassword req) {
        authService.forgotPassword(req.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordRequests.ResetPassword req) {
        authService.resetPassword(SecurityUtil.getCurrentUserId(), req.getPassword());
        return ResponseEntity.noContent().build();
    }

    /** Kicks off the OAuth2 login flow (e.g. provider=google) via Spring Security. */
    @GetMapping("/oauth/{provider}")
    public ResponseEntity<Void> oauth(@PathVariable String provider) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(java.net.URI.create("/oauth2/authorization/" + provider))
                .build();
    }
}
