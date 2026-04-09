package com.example.orderservice.config.security;

import java.util.UUID;

/**
 * Rappresenta l'utente autenticato estratto dal JWT.
 * Immutabile — viene creato una volta per request e
 * inserito nel SecurityContext.
 */
public record AuthenticatedUser(
        UUID customerId,
        String role         // "CUSTOMER" | "ADMIN"
) {}