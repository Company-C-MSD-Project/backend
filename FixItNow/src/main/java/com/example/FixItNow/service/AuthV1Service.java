package com.example.FixItNow.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.dto.v1.AuthSessionV1;
import com.example.FixItNow.dto.v1.AuthUserV1;
import com.example.FixItNow.dto.v1.LoginRequestV1;
import com.example.FixItNow.dto.v1.SignupRequestV1;
import com.example.FixItNow.entity.RefreshToken;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.exception.ResourceNotFoundException;
import com.example.FixItNow.exception.UnauthorizedException;
import com.example.FixItNow.repository.UserRepository;
import com.example.FixItNow.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * Auth flows for the /api/v1/auth contract consumed by the frontend
 * (lib/auth-api.ts). Issues a stateless access token plus a stored refresh
 * token, and maps the domain UserType to the UI role strings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthV1Service {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final ProviderRequestV1Service providerRequestService;

    @Transactional
    public AuthSessionV1 signup(SignupRequestV1 req) {
        // Generic message so signup cannot be used to enumerate existing accounts.
        String username = deriveUsername(req.getUsername(), req.getEmail());
        if (userRepository.existsByEmail(req.getEmail()) || userRepository.existsByUsername(username)) {
            throw new BadRequestException("An account with that email or username already exists.");
        }

        UserType userType = toUserType(req.getRole());
        User user = User.builder()
                .name(username)
                .email(req.getEmail())
                .username(username)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .userType(userType)
                .isActive(true)
                // Providers must be verified by an admin before accepting jobs.
                .isVerified(userType != UserType.SERVICE_PROVIDER)
                .build();

        user = userRepository.save(user);
        log.info("Signup ({}) for {}", userType, user.getEmail());

        // Providers must be reviewed by an admin — open an application.
        if (userType == UserType.SERVICE_PROVIDER) {
            providerRequestService.createForProvider(user, null);
        }
        return buildSession(user);
    }

    @Transactional
    public AuthSessionV1 login(LoginRequestV1 req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (AuthenticationException ex) {
            // Single message for unknown account / bad password — no enumeration.
            throw new UnauthorizedException("Invalid email or password.");
        }

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));
        log.info("Login for {}", user.getEmail());
        return buildSession(user);
    }

    /**
     * Find-or-create a user from a verified Google identity and issue a session.
     * Google-verified accounts are created as active+verified HOMEOWNERs with a
     * random local password (they always sign in via Google).
     */
    @Transactional
    public AuthSessionV1 oauthLogin(String email, String name) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Google account did not provide an email.");
        }
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            String username = deriveUsername(null, email);
            User created = User.builder()
                    .name(name != null && !name.isBlank() ? name : username)
                    .email(email)
                    .username(username)
                    .passwordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                    .userType(UserType.HOMEOWNER)
                    .isActive(true)
                    .isVerified(true)
                    .build();
            log.info("OAuth signup (google) for {}", email);
            return userRepository.save(created);
        });
        return buildSession(user);
    }

    /** Exchange a valid refresh token for a new access token (rotates the refresh token). */
    @Transactional
    public AuthSessionV1 refresh(String refreshToken) {
        RefreshToken current = refreshTokenService.verify(refreshToken);
        RefreshToken rotated = refreshTokenService.rotate(current);
        User user = rotated.getUser();
        String access = tokenProvider.generateAccessToken(user.getEmail());
        return AuthSessionV1.builder()
                .accessToken(access)
                .refreshToken(rotated.getToken())
                .user(toAuthUser(user))
                .build();
    }

    @Transactional(readOnly = true)
    public AuthUserV1 session(Long userId) {
        return toAuthUser(requireUser(userId));
    }

    @Transactional
    public void logout(Long userId) {
        if (userId != null) {
            refreshTokenService.revokeAllForUser(userId);
        }
    }

    /** Best-effort: never reveals whether the email exists (no enumeration). */
    @Transactional(readOnly = true)
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(u ->
                log.info("Password reset requested for {} (email dispatch is a later step)", u.getEmail()));
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = requireUser(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        // Invalidate existing sessions after a password change.
        refreshTokenService.revokeAllForUser(userId);
        log.info("Password reset for {}", user.getEmail());
    }

    // ----- helpers -----

    private AuthSessionV1 buildSession(User user) {
        String access = tokenProvider.generateAccessToken(user.getEmail());
        RefreshToken refresh = refreshTokenService.issue(user);
        return AuthSessionV1.builder()
                .accessToken(access)
                .refreshToken(refresh.getToken())
                .user(toAuthUser(user))
                .build();
    }

    private AuthUserV1 toAuthUser(User user) {
        return AuthUserV1.builder()
                .id(String.valueOf(user.getId()))
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getName())
                .avatarUrl(null)
                .role(uiRole(user.getUserType()))
                .build();
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new UnauthorizedException("Not authenticated.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private String deriveUsername(String requested, String email) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        String base = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    private String uiRole(UserType type) {
        if (type == null) return "homeowner";
        return switch (type) {
            case SERVICE_PROVIDER -> "provider";
            case ADMIN -> "admin";
            case HOMEOWNER -> "homeowner";
        };
    }

    private UserType toUserType(String role) {
        if (role == null) return UserType.HOMEOWNER;
        // Self-serve signup can only create homeowners or providers; admins are provisioned separately.
        return "provider".equalsIgnoreCase(role.trim()) ? UserType.SERVICE_PROVIDER : UserType.HOMEOWNER;
    }
}
