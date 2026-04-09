package com.example.orderservice.service;

import com.example.orderservice.domain.enums.OrderStatus;
import com.example.orderservice.exception.IllegalOrderStateException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class OrderStateMachine {

    // Mappa: stato corrente → stati raggiungibili
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS =
            new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(OrderStatus.PENDING,   EnumSet.of(
                OrderStatus.CONFIRMED,
                OrderStatus.CANCELLED));

        TRANSITIONS.put(OrderStatus.CONFIRMED, EnumSet.of(
                OrderStatus.SHIPPED,
                OrderStatus.CANCELLED));

        TRANSITIONS.put(OrderStatus.SHIPPED,   EnumSet.of(
                OrderStatus.DELIVERED));

        // Stati terminali: nessuna transizione consentita
        TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    /**
     * Valida e applica la transizione.
     * Lancia IllegalOrderStateException se la transizione non è consentita.
     */
    public void validateTransition(OrderStatus current, OrderStatus target) {
        Set<OrderStatus> allowed = TRANSITIONS.getOrDefault(
                current, EnumSet.noneOf(OrderStatus.class));

        if (!allowed.contains(target)) {
            throw new IllegalOrderStateException(current, target);
        }
    }

    public boolean canTransition(OrderStatus current, OrderStatus target) {
        return TRANSITIONS
                .getOrDefault(current, EnumSet.noneOf(OrderStatus.class))
                .contains(target);
    }

    public Set<OrderStatus> allowedTransitions(OrderStatus current) {
        return TRANSITIONS.getOrDefault(
                current, EnumSet.noneOf(OrderStatus.class));
    }
}