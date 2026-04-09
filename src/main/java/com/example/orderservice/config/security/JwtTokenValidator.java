package com.example.orderservice.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Responsabilità unica: validare il JWT e estrarne i claim.
 * Non emette token — solo validazione.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenValidator {

    private final JwtProperties jwtProperties;

    /**
     * Valida la firma e la scadenza del token,
     * poi costruisce AuthenticatedUser dai claim.
     *
     * @throws JwtException se il token è invalido o scaduto
     */
    public AuthenticatedUser validate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID customerId = UUID.fromString(
                claims.get(jwtProperties.getCustomerIdClaim(), String.class));

        String role = extractRole(claims);

        return new AuthenticatedUser(customerId, role);
    }

    private String extractRole(Claims claims) {
        Object raw = claims.get(jwtProperties.getRolesClaim());

        // Il claim roles può essere una stringa singola o una lista
        if (raw instanceof List<?> list && !list.isEmpty()) {
            return list.get(0).toString()
                    .replace("ROLE_", "");
        }
        if (raw instanceof String str) {
            return str.replace("ROLE_", "");
        }

        // Default a CUSTOMER se il claim è assente
        return "CUSTOMER";
    }

    private SecretKey secretKey() {
        byte[] decoded = Base64.getDecoder()
                .decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(decoded);
    }
}