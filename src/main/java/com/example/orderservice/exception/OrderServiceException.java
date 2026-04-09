package com.example.orderservice.exception;

import java.util.UUID;

// ── Base exception ────────────────────────────────────────────
public abstract class OrderServiceException extends RuntimeException {
    protected OrderServiceException(String message) {
        super(message);
    }
}