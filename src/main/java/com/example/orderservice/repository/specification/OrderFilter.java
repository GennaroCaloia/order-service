package com.example.orderservice.repository.specification;

import com.example.orderservice.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * Trasportatore di filtri dalla request HTTP fino alla Specification.
 * Tutti i campi sono nullable: null = filtro non applicato.
 */
@Builder
public record OrderFilter(
        UUID customerId,
        OrderStatus status,
        Instant from,
        Instant to
) {}