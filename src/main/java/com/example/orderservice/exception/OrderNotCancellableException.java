package com.example.orderservice.exception;

import java.util.UUID;

public class OrderNotCancellableException extends OrderServiceException {
    public OrderNotCancellableException(UUID orderId,
                                        com.example.orderservice.domain.enums.OrderStatus status) {
        super("L'ordine %s non può essere cancellato nello stato %s"
                .formatted(orderId, status));
    }
}