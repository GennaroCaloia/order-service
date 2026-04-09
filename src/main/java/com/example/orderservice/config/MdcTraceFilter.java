package com.example.orderservice.config;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Propaga traceId e spanId dal contesto di Micrometer Tracing
 * al MDC di SLF4J, rendendoli disponibili:
 * - Nei log (logback-spring.xml li include automaticamente)
 * - Nella risposta di errore (GlobalExceptionHandler li legge da MDC)
 * - Nell'header di risposta X-Trace-Id (utile per il debug lato client)
 */
@Component
@Order(1)   // eseguito prima del JwtAuthenticationFilter
@RequiredArgsConstructor
public class MdcTraceFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final Tracer tracer;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        Span currentSpan = tracer.currentSpan();

        try {
            if (currentSpan != null) {
                String traceId = currentSpan.context().traceId();
                String spanId  = currentSpan.context().spanId();

                MDC.put("traceId", traceId);
                MDC.put("spanId",  spanId);

                // Header di risposta — consente al client di correlare
                // la propria request con i log del server
                response.addHeader(TRACE_ID_HEADER, traceId);
            }

            chain.doFilter(request, response);

        } finally {
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }
}