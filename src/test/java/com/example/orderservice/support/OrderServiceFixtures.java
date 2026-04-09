package com.example.orderservice.support;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderItem;
import com.example.orderservice.domain.enums.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.OrderItemRequest;
import com.example.orderservice.dto.request.UpdateOrderStatusRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Factory centralizzata per oggetti di test.
 * Evita la duplicazione di builder nei test e
 * rende esplicite le assunzioni sui dati di default.
 */
public final class OrderServiceFixtures {

    public static final UUID CUSTOMER_ID =
            UUID.fromString("a0000000-0000-0000-0000-000000000001");

    public static final UUID ORDER_ID =
            UUID.fromString("c0000000-0000-0000-0000-000000000001");

    public static final UUID PRODUCT_ID =
            UUID.fromString("b0000000-0000-0000-0000-000000000001");

    private OrderServiceFixtures() {}

    // ── Entità ────────────────────────────────────────────────

    public static Order pendingOrder() {
        Order order = Order.builder()
                .id(ORDER_ID)
                .customerId(CUSTOMER_ID)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("149.98"))
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        OrderItem item = orderItem(order);
        order.addItem(item);
        return order;
    }

    public static Order orderWithStatus(OrderStatus status) {
        Order order = pendingOrder();
        order.setStatus(status);
        return order;
    }

    public static OrderItem orderItem(Order order) {
        return OrderItem.builder()
                .id(UUID.randomUUID())
                .order(order)
                .productId(PRODUCT_ID)
                .productName("Laptop Pro 15\"")
                .quantity(1)
                .unitPrice(new BigDecimal("149.98"))
                .subtotal(new BigDecimal("149.98"))
                .createdAt(Instant.now())
                .build();
    }

    // ── Request DTO ───────────────────────────────────────────

    public static CreateOrderRequest createOrderRequest() {
        return CreateOrderRequest.builder()
                .customerId(CUSTOMER_ID)
                .items(List.of(orderItemRequest()))
                .notes("Test order")
                .build();
    }

    public static OrderItemRequest orderItemRequest() {
        return OrderItemRequest.builder()
                .productId(PRODUCT_ID)
                .productName("Laptop Pro 15\"")
                .quantity(1)
                .unitPrice(new BigDecimal("149.98"))
                .build();
    }

    public static UpdateOrderStatusRequest updateStatusRequest(OrderStatus status) {
        return UpdateOrderStatusRequest.builder()
                .status(status)
                .reason("Test reason")
                .build();
    }
}