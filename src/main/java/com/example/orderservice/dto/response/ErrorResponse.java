package com.example.orderservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

/**
 * Struttura di errore uniforme per tutti gli endpoint.
 * I campi nullable sono esclusi dalla serializzazione JSON
 * quando assenti (es. fieldErrors solo su 400).
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        List<FieldError> fieldErrors   // popolato solo su errori di validazione
) {
    /**
     * Dettaglio di un singolo campo non valido.
     */
    @Builder
    public record FieldError(
            String field,
            Object rejectedValue,
            String message
    ) {}
}