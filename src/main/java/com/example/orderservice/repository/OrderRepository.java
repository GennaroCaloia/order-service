package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.enums.OrderStatus;
import com.example.orderservice.repository.projection.OrderSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository
        extends JpaRepository<Order, UUID>,
        JpaSpecificationExecutor<Order> {

    // ── Query custom ──────────────────────────────────────────

    // JOIN FETCH esplicito: carica items nella stessa query
    // evitando il problema N+1 quando serve l'ordine completo.
    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.id = :id
            """)
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    // Tutti gli ordini di un customer, paginati.
    // Spring Data genera la countQuery automaticamente.
    @Query("""
            SELECT o FROM Order o
            WHERE o.customerId = :customerId
            ORDER BY o.createdAt DESC
            """)
    Page<Order> findByCustomerId(
            @Param("customerId") UUID customerId,
            Pageable pageable);

    // Ordini per status creati negli ultimi N giorni.
    // Instant è compatibile con TIMESTAMPTZ di PostgreSQL.
    @Query("""
            SELECT o FROM Order o
            WHERE o.status = :status
              AND o.createdAt >= :since
            ORDER BY o.createdAt DESC
            """)
    List<Order> findByStatusSince(
            @Param("status") OrderStatus status,
            @Param("since") Instant since);

    // Conta ordini attivi (non terminali) per un customer.
    // Utile per business rule: "max N ordini aperti per customer".
    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.customerId = :customerId
              AND o.status NOT IN ('DELIVERED', 'CANCELLED')
            """)
    long countActiveByCustomerId(@Param("customerId") UUID customerId);

    // ── Proiezione ────────────────────────────────────────────

    // Restituisce solo i campi necessari per la lista-summary,
    // senza caricare gli items. La countQuery separata è
    // necessaria perché Spring Data non può derivarla
    // automaticamente da una query con SELECT su proiezione.
    @Query(
            value = """
                SELECT o.id        AS id,
                       o.customerId AS customerId,
                       o.status    AS status,
                       o.totalAmount AS totalAmount,
                       o.createdAt AS createdAt
                FROM Order o
                WHERE o.customerId = :customerId
                ORDER BY o.createdAt DESC
                """,
            countQuery = """
                SELECT COUNT(o) FROM Order o
                WHERE o.customerId = :customerId
                """
    )
    Page<OrderSummary> findSummariesByCustomerId(
            @Param("customerId") UUID customerId,
            Pageable pageable);

    // ── Specification (filtri dinamici) ───────────────────────
    // I metodi findAll(Specification, Pageable) sono ereditati
    // da JpaSpecificationExecutor — non servono dichiarazioni
    // aggiuntive qui.
}