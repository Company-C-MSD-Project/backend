package com.example.FixItNow.config;

import org.springframework.context.annotation.Bean;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // Category endpoints (read-only public)
                .requestMatchers("GET", "/api/categories/**").permitAll()
                // Service endpoints (read-only public)
                .requestMatchers("GET", "/api/services/**").permitAll()
                // Secured endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("POST", "/api/bookings/**").hasAnyRole("HOMEOWNER", "SERVICE_PROVIDER")
                .requestMatchers("GET", "/api/bookings/**").hasAnyRole("HOMEOWNER", "SERVICE_PROVIDER", "ADMIN")
                .requestMatchers("/api/profile/**").authenticated()
                .anyRequest().authenticated()
            )
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                corsConfig.setAllowedOrigins(java.util.Arrays.asList("http://localhost:3000", "http://localhost:8080"));
                corsConfig.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                corsConfig.setAllowedHeaders(java.util.Arrays.asList("*"));
                corsConfig.setAllowCredentials(true);
                return corsConfig;
            }));

        // JWT filter will be added here in JwtSecurityFilter
        // http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
