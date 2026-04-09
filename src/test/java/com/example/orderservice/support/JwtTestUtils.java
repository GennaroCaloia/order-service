package com.example.orderservice.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Genera JWT validi firmati con la stessa chiave
 * configurata in application-test.yml.
 * Solo per uso nei test — mai in produzione.
 */
public final class JwtTestUtils {

    // Deve corrispondere a app.security.jwt.secret in application-test.yml
    static final String TEST_SECRET =
            "dGhpcyBpcyBhIHZlcnkgbG9uZyBzZWNyZXQga2V5IGZvciBvcmRlciBzZXJ2aWNl";

    private JwtTestUtils() {}

    public static String customerToken(UUID customerId) {
        return buildToken(customerId, "CUSTOMER");
    }

    public static String adminToken() {
        return buildToken(
                UUID.fromString("f0000000-0000-0000-0000-000000000001"),
                "ADMIN");
    }

    private static String buildToken(UUID subject, String role) {
        return Jwts.builder()
                .claim("sub", subject.toString())
                .claim("roles", List.of(role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(secretKey())
                .compact();
    }

    private static SecretKey secretKey() {
        return Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(TEST_SECRET));
    }
}