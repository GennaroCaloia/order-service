package com.example.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record CreateOrderRequest(

        @NotNull(message = "customerId è obbligatorio")
        UUID customerId,

        @NotNull(message = "La lista degli item è obbligatoria")
        @Size(min = 1, max = 50, message = "Un ordine deve contenere tra 1 e 50 item")
        @Valid                          // propaga la validazione agli elementi della lista
        List<OrderItemRequest> items,

        @Size(max = 500, message = "Le note non possono superare 500 caratteri")
        String notes
) {}