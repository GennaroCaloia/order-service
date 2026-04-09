package com.example.orderservice.health;

import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator custom che verifica la raggiungibilità
 * del database con una query leggera.
 * Esposto su /actuator/health con dettaglio "orderDatabase".
 */
@Component("orderDatabase")
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

    private final OrderRepository orderRepository;

    @Override
    public Health health() {
        try {
            long count = orderRepository.count();
            return Health.up()
                    .withDetail("ordersCount", count)
                    .withDetail("status", "Database raggiungibile")
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}