package com.example.orderservice.config.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Token di autenticazione che porta AuthenticatedUser
 * come principal nel SecurityContext.
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedUser principal;

    public JwtAuthenticationToken(AuthenticatedUser principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;    // il token JWT non viene mantenuto in memoria
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return principal;
    }
}