package com.example.FixItNow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.FixItNow.dto.request.LoginRequest;
import com.example.FixItNow.dto.request.RegisterRequest;
import com.example.FixItNow.dto.response.AuthResponse;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.repository.UserRepository;
import com.example.FixItNow.security.JwtTokenProvider;

/**
 * Handles user registration and login (SRS FR1, FR2).
 * Registration creates a User row with BCrypt-hashed password.
 * Login authenticates credentials, then returns a signed JWT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public User register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email address is already registered.");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BadRequestException("Username is already taken.");
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .username(req.getUsername())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .address(req.getAddress())
                .userType(req.getUserType())
                .isActive(true)
                .build();

        // Set role-specific fields
        if (req.getUserType() == UserType.SERVICE_PROVIDER) {
            user.setServiceCategory(req.getServiceCategory());
            // Providers start unverified — admin must approve before they can accept jobs
            user.setVerified(false);
        }
        if (req.getUserType() == UserType.ADMIN) {
            user.setDepartment(req.getDepartment());
        }

        log.info("Registering new {} with email: {}", req.getUserType(), req.getEmail());
        return userRepository.save(user);
    }

    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getUsernameOrEmail(), req.getPassword()));

        String token = tokenProvider.generateToken(auth);
        User user    = userRepository.findByEmail(req.getUsernameOrEmail())
                .or(() -> userRepository.findByUsername(req.getUsernameOrEmail()))
                .orElseThrow();

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .userType(user.getUserType())
                .build();
    }
}