package com.example.orderservice.config.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Helper per accedere all'utente autenticato dal SecurityContext
 * senza boilerplate nei service e nei controller.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<AuthenticatedUser> currentUser() {
        Authentication auth = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(jwtAuth.getPrincipal());
        }
        return Optional.empty();
    }

    public static UUID currentCustomerId() {
        return currentUser()
                .map(AuthenticatedUser::customerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Nessun utente autenticato nel contesto corrente"));
    }

    public static boolean isAdmin() {
        return currentUser()
                .map(u -> "ADMIN".equals(u.role()))
                .orElse(false);
    }
}