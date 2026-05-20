package com.example.FixItNow.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.FixItNow.entity.User;
import com.example.FixItNow.repository.UserRepository;

import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * Loads user details from the database for Spring Security authentication.
 * Maps the UserType enum to a Spring Security ROLE_ authority for RBAC.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + usernameOrEmail));

        // Derive Spring Security authority from UserType enum (e.g., ROLE_HOMEOWNER)
        String role = "ROLE_" + user.getUserType().name();

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.isActive(),
                true, true, true,
                List.of(new SimpleGrantedAuthority(role))
        );
    }
}

