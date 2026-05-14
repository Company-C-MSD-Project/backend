package com.example.FixItNow.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

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
     * Get the ID of the currently authenticated user from SecurityContext.
     * (Assumes ID is stored in a custom claim or principal)
     */
    public static Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.User) {
            // For a real implementation, you'd extract the user ID from the principal
            // This is a placeholder
            return null;
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
