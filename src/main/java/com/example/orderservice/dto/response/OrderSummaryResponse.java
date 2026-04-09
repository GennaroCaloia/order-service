package com.example.orderservice.dto.response;

import com.example.orderservice.domain.enums.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO leggero per la lista paginata — senza items.
 * Mappato sia dall'entità Order che dalla proiezione OrderSummary.
 */
@Builder
public record OrderSummaryResponse(
        UUID id,
        UUID customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt
) {}