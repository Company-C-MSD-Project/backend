package com.example.FixItNow.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.FixItNow.entity.User;
import com.example.FixItNow.repository.UserRepository;

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

        // UserPrincipal carries the db id + ROLE_<UserType> authority for RBAC.
        return new UserPrincipal(user);
    }
}

