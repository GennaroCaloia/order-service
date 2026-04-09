package com.example.orderservice.dto.response;

import com.example.orderservice.domain.enums.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
public record OrderResponse(
        UUID id,
        UUID customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String notes,
        List<OrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt,
        Long version            // esposto per supportare ottimistic locking lato client
) {}