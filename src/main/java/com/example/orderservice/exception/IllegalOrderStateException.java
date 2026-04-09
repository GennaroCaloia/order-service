package com.example.orderservice.exception;

import com.example.orderservice.domain.enums.OrderStatus;

public class IllegalOrderStateException extends OrderServiceException {
    public IllegalOrderStateException(OrderStatus current, OrderStatus target) {
        super("Transizione di stato non consentita: %s → %s"
                .formatted(current, target));
    }
}