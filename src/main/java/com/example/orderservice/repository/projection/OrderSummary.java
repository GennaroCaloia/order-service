package com.example.orderservice.repository.projection;

import com.example.orderservice.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Proiezione interface-based per la lista ordini.
 * Spring Data genera un proxy a runtime che mappa
 * ogni getter sul campo corrispondente del risultato JPQL.
 *
 * I nomi dei metodi devono corrispondere ESATTAMENTE
 * agli alias definiti nella query (case-sensitive).
 */
public interface OrderSummary {

    UUID getId();
    UUID getCustomerId();
    OrderStatus getStatus();
    BigDecimal getTotalAmount();
    Instant getCreatedAt();
}