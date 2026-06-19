package com.example.FixItNow.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.FixItNow.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.FixItNow.security.JwtAuthFilter;
import com.example.FixItNow.security.OAuth2LoginSuccessHandler;

/**
 * Spring Security configuration (SRS §2.5 Authentication & RBAC).
 * - JWT-based stateless authentication
 * - Role-based access control (RBAC) for three roles: HOMEOWNER, SERVICE_PROVIDER, ADMIN
 * - CSRF disabled (stateless API, no cookies)
 * - CORS enabled for client-side requests
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /** Allowed CORS origins, comma-separated, sourced from config/env. */
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    /** Google OAuth client id; when blank, "Sign in with Google" is disabled and the app still boots. */
    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                           HttpCookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepo,
                                           OAuth2LoginSuccessHandler oauth2SuccessHandler) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                // v1 auth: only the unauthenticated flows are public; logout,
                // session, and password/reset fall through to authenticated().
                .requestMatchers("/api/v1/auth/signup", "/api/v1/auth/login",
                        "/api/v1/auth/refresh", "/api/v1/auth/password/forgot").permitAll()
                .requestMatchers("/api/v1/auth/oauth/**").permitAll()
                // Spring Security OAuth2 login endpoints (redirect to Google + callback)
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/paypal/webhook").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // Category endpoints (read-only public)
                .requestMatchers(HttpMethod.GET,"/api/categories/**").permitAll()
                // Service endpoints (read-only public)
                .requestMatchers(HttpMethod.GET,"/api/services/**").permitAll()
                // v1 public browse (categories, sub-services, providers)
                .requestMatchers(HttpMethod.GET,"/api/v1/service-categories/**").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/sub-services/**").permitAll()
                .requestMatchers(HttpMethod.GET,"/api/v1/providers/**").permitAll()
                // Public news (read-only) and uploaded media
                .requestMatchers(HttpMethod.GET,"/api/v1/news", "/api/v1/news/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                // Secured endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Root-level admin stats (axios http client) — admin only
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST,"/api/bookings/**").hasAnyRole("HOMEOWNER", "SERVICE_PROVIDER")
                .requestMatchers(HttpMethod.GET,"/api/bookings/**").hasAnyRole("HOMEOWNER", "SERVICE_PROVIDER", "ADMIN")
                .requestMatchers("/api/profile/**").authenticated()
                .anyRequest().authenticated()
            )
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                corsConfig.setAllowedOrigins(allowedOrigins);
                corsConfig.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                corsConfig.setAllowedHeaders(java.util.Arrays.asList("*"));
                corsConfig.setAllowCredentials(true);
                return corsConfig;
            }));

        // "Sign in with Google" — only enabled when a client-id is configured, so the
        // app still boots without Google credentials. Stateless: the in-flight auth
        // request is carried in a cookie rather than an HTTP session.
        if (googleClientId != null && !googleClientId.isBlank()) {
            http.oauth2Login(oauth -> oauth
                    .authorizationEndpoint(a -> a.authorizationRequestRepository(cookieAuthRequestRepo))
                    .successHandler(oauth2SuccessHandler));
        }

        // Validate the Bearer JWT on every request and populate the SecurityContext
        // before the username/password filter runs (stateless auth).
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
