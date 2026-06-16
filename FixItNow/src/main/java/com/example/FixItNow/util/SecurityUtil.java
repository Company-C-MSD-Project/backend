package com.example.FixItNow.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.FixItNow.security.UserPrincipal;

/**
 * Utility to get current authenticated user from Security Context.
 */
@Component
public class SecurityUtil {

    /**
     * Get the username of the currently authenticated user.
     */
    public static String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * Get the ID of the currently authenticated user from the SecurityContext.
     * Returns null when there is no authenticated UserPrincipal (e.g., anonymous request).
     */
    public static Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        return null;
    }

    /**
     * Check if the current user has a specific role.
     */
    public static boolean hasRole(String role) {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
