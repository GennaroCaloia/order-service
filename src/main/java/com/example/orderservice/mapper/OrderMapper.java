package com.example.orderservice.mapper;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderItem;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.OrderItemRequest;
import com.example.orderservice.dto.response.OrderItemResponse;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.OrderSummaryResponse;
import com.example.orderservice.repository.projection.OrderSummary;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",          // genera un @Component Spring
        nullValuePropertyMappingStrategy    // ignora null negli update parziali
                = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy                // build fallisce se un campo target
                = ReportingPolicy.ERROR         // non ha sorgente — configurato anche via compiler arg
)
public interface OrderMapper {

    // ── Order → OrderResponse ─────────────────────────────────

    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(Order order);

    // ── Order → OrderSummaryResponse ──────────────────────────

    @Mapping(target = "id",          source = "id")
    @Mapping(target = "customerId",  source = "customerId")
    @Mapping(target = "status",      source = "status")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "createdAt",   source = "createdAt")
    OrderSummaryResponse toSummaryResponse(Order order);

    // Overload per la proiezione interface-based dal repository.
    // MapStruct invoca i getter del proxy generato da Spring Data.
    OrderSummaryResponse toSummaryResponse(OrderSummary projection);

    // ── List mapping ──────────────────────────────────────────

    List<OrderSummaryResponse> toSummaryResponseList(List<Order> orders);

    // ── OrderItem → OrderItemResponse ─────────────────────────

    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);

    // ── CreateOrderRequest → Order ────────────────────────────

    @Mapping(target = "id",          ignore = true)  // generato da JPA
    @Mapping(target = "status",      ignore = true)  // impostato dal service
    @Mapping(target = "totalAmount", ignore = true)  // calcolato dal service
    @Mapping(target = "createdAt",   ignore = true)  // gestito da @CreatedDate
    @Mapping(target = "updatedAt",   ignore = true)  // gestito da @LastModifiedDate
    @Mapping(target = "version",     ignore = true)  // gestito da @Version
    @Mapping(target = "items",       ignore = true)  // aggiunto dal service via addItem()
    Order toEntity(CreateOrderRequest request);

    // ── OrderItemRequest → OrderItem ──────────────────────────

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "order",     ignore = true)  // impostato dal service via addItem()
    @Mapping(target = "subtotal",  ignore = true)  // calcolato da @PrePersist
    @Mapping(target = "createdAt", ignore = true)
    OrderItem toItemEntity(OrderItemRequest request);

    List<OrderItem> toItemEntityList(List<OrderItemRequest> requests);

    // ── Update parziale di Order ──────────────────────────────

    /**
     * Aggiorna i campi mutabili di un'entità esistente.
     * Con NullValuePropertyMappingStrategy.IGNORE i campi null
     * nella request non sovrascrivono i valori esistenti.
     * @MappingTarget indica a MapStruct di modificare l'istanza
     * passata invece di crearne una nuova.
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "customerId",  ignore = true)
    @Mapping(target = "status",      ignore = true)  // gestito dalla state machine
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "items",       ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    @Mapping(target = "version",     ignore = true)
    void updateEntity(CreateOrderRequest request, @MappingTarget Order order);
}