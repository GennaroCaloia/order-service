package com.example.orderservice.exception;

import java.util.UUID;

public class OrderNotFoundException extends OrderServiceException {
    public OrderNotFoundException(UUID orderId) {
        super("Ordine non trovato: " + orderId);
    }
}