package com.example.orderservice.dto.request;

import com.example.orderservice.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateOrderStatusRequest(

        @NotNull(message = "Il nuovo status è obbligatorio")
        OrderStatus status,

        @jakarta.validation.constraints.Size(max = 500)
        String reason          // motivo opzionale (es. per CANCELLED)
) {}