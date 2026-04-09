package com.example.orderservice.config.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenValidator tokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Nessun header Authorization → prosegui senza autenticazione.
        // SecurityFilterChain deciderà se la route richiede auth.
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {
            AuthenticatedUser user = tokenValidator.validate(token);

            // Popola il SecurityContext
            JwtAuthenticationToken auth = new JwtAuthenticationToken(user);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Scrivi customerId nel MDC per la correlazione nei log
            MDC.put("customerId", user.customerId().toString());

            chain.doFilter(request, response);

        } catch (JwtException ex) {
            log.warn("JWT non valido: {}", ex.getMessage());
            sendUnauthorized(response, ex.getMessage());
        } finally {
            // Pulizia MDC obbligatoria — i thread tornano nel pool
            MDC.remove("customerId");
            SecurityContextHolder.clearContext();
        }
    }

    private void sendUnauthorized(HttpServletResponse response,
                                  String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                """
                {"status":401,"error":"Unauthorized","message":"%s"}
                """.formatted(message.replace("\"", "'")));
    }
}