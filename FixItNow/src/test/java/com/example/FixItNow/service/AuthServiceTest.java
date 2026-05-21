package com.example.FixItNow.service;

import com.example.FixItNow.dto.request.LoginRequest;
import com.example.FixItNow.dto.request.RegisterRequest;
import com.example.FixItNow.dto.response.AuthResponse;
import com.example.FixItNow.entity.User;
import com.example.FixItNow.enums.UserType;
import com.example.FixItNow.exception.BadRequestException;
import com.example.FixItNow.repository.UserRepository;
import com.example.FixItNow.security.JwtTokenProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository        userRepository;
    @Mock private PasswordEncoder       passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider      tokenProvider;

    @InjectMocks private AuthService authService;

    private RegisterRequest validRequest;
    private User            savedUser;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setName("Alice Smith");
        validRequest.setEmail("alice@example.com");
        validRequest.setUsername("alice123");
        validRequest.setPassword("SecurePass1");
        validRequest.setUserType(UserType.HOMEOWNER);

        savedUser = User.builder()
                .id(1L)
                .name("Alice Smith")
                .email("alice@example.com")
                .username("alice123")
                .passwordHash("$2a$hashed")
                .userType(UserType.HOMEOWNER)
                .isActive(true)
                .build();
    }

    // register() 

    /**
     * UT_01 – Valid name, unique email, unique username,
     *         password ≥ 8 chars, HOMEOWNER userType.
     */
    @Test
    @DisplayName("UT_01: register() – valid HOMEOWNER registration should save and return user")
    void ut01_register_validHomeowner_shouldSaveAndReturnUser() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice123")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass1")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.register(validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getUserType()).isEqualTo(UserType.HOMEOWNER);
        verify(userRepository).save(any(User.class));
    }

    /**
     * UT_02 – Email already registered should throw BadRequestException.
     */
    @Test
    @DisplayName("UT_02: register() – duplicate email should throw BadRequestException")
    void ut02_register_duplicateEmail_shouldThrow() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    /**
     * UT_03 – Null password should not persist any user.
     */
    @Test
    @DisplayName("UT_03: register() – null password should throw and not persist user")
    void ut03_register_nullPassword_shouldNotPersist() {
        validRequest.setPassword(null);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(null))
                .thenThrow(new IllegalArgumentException("Password cannot be null"));

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(Exception.class);

        verify(userRepository, never()).save(any());
    }

    //  login() 

    /**
     * UT_04 – Valid email + matching password should return an AuthResponse
     *         containing a JWT token.
     */
    @Test
    @DisplayName("UT_04: login() – correct credentials should return AuthResponse with token")
    void ut04_login_validCredentials_shouldReturnAuthResponse() {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("alice@example.com");
        req.setPassword("SecurePass1");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("mock.jwt.token");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(savedUser));

        AuthResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    /**
     * UT_05 – Correct email but wrong password should throw BadCredentialsException.
     */
    @Test
    @DisplayName("UT_05: login() – wrong password should throw BadCredentialsException")
    void ut05_login_wrongPassword_shouldThrow() {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("alice@example.com");
        req.setPassword("WrongPass!");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    /**
     * UT_06 – Email not present in database should fail at AuthenticationManager.
     */
    @Test
    @DisplayName("UT_06: login() – email not in database should throw BadCredentialsException")
    void ut06_login_unknownEmail_shouldThrow() {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail("ghost@example.com");
        req.setPassword("AnyPass1");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("User not found"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }
}
