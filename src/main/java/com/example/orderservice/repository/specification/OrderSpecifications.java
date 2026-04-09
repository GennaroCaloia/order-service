package com.example.orderservice.repository.specification;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.enums.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Factory di Specification per filtri dinamici su Order.
 * Ogni metodo statico produce un predicato componibile:
 *
 *   Specification<Order> spec = OrderSpecifications.forCustomer(id)
 *       .and(OrderSpecifications.withStatus(OrderStatus.PENDING))
 *       .and(OrderSpecifications.createdAfter(since));
 */
public final class OrderSpecifications {

    private OrderSpecifications() {}

    public static Specification<Order> forCustomer(UUID customerId) {
        return (root, query, cb) ->
                customerId == null
                        ? cb.conjunction()
                        : cb.equal(root.get("customerId"), customerId);
    }

    public static Specification<Order> withStatus(OrderStatus status) {
        return (root, query, cb) ->
                status == null
                        ? cb.conjunction()
                        : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> createdAfter(Instant from) {
        return (root, query, cb) ->
                from == null
                        ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Order> createdBefore(Instant to) {
        return (root, query, cb) ->
                to == null
                        ? cb.conjunction()
                        : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    /**
     * Combina tutti i filtri opzionali in un'unica Specification.
     * Parametri null vengono ignorati (conjunction = sempre vero).
     * Comodo nel service layer per non costruire la chain manualmente.
     */
    public static Specification<Order> buildFilter(
            UUID customerId,
            OrderStatus status,
            Instant from,
            Instant to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (customerId != null) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}