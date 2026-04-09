package com.example.orderservice.repository;

import com.example.orderservice.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    // Delete bulk per orderId — più efficiente del cascade JPA
    // quando si vuole rimuovere tutti gli item senza caricarli
    // prima in memoria (es. operazione admin di pulizia).
    @Modifying
    @Query("DELETE FROM OrderItem oi WHERE oi.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") UUID orderId);

    // Verifica se un prodotto è presente in ordini attivi.
    // Utile prima di disattivare un prodotto nel catalogo.
    @Query("""
            SELECT COUNT(oi) > 0 FROM OrderItem oi
            JOIN oi.order o
            WHERE oi.productId = :productId
              AND o.status NOT IN ('DELIVERED', 'CANCELLED')
            """)
    boolean existsInActiveOrdersByProductId(@Param("productId") UUID productId);
}