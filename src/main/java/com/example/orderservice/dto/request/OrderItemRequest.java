package com.example.orderservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderItemRequest(

        @NotNull(message = "productId è obbligatorio")
        UUID productId,

        @NotBlank(message = "Il nome del prodotto è obbligatorio")
        @Size(max = 255, message = "Il nome del prodotto non può superare 255 caratteri")
        String productName,

        @NotNull(message = "La quantità è obbligatoria")
        @Min(value = 1, message = "La quantità minima è 1")
        @Max(value = 999, message = "La quantità massima per riga è 999")
        Integer quantity,

        @NotNull(message = "Il prezzo unitario è obbligatorio")
        @DecimalMin(value = "0.01", message = "Il prezzo unitario deve essere maggiore di zero")
        @DecimalMax(value = "99999.99", message = "Il prezzo unitario non può superare 99999.99")
        @Digits(integer = 7, fraction = 2, message = "Formato prezzo non valido (max 7 interi, 2 decimali)")
        BigDecimal unitPrice
) {}