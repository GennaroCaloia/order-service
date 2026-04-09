package com.example.orderservice.service.impl;

import com.example.orderservice.config.security.SecurityUtils;
import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.entity.OrderItem;
import com.example.orderservice.domain.enums.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.UpdateOrderStatusRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.OrderSummaryResponse;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.exception.OrderNotCancellableException;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.metrics.OrderMetrics;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.specification.OrderFilter;
import com.example.orderservice.repository.specification.OrderSpecifications;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.service.OrderStateMachine;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository    orderRepository;
    private final OrderMapper        orderMapper;
    private final OrderStateMachine  stateMachine;
    private final OrderMetrics orderMetrics;

    // ── createOrder ── aggiornato ─────────────────────────────────
    @Override
    @Transactional
    @Observed(
            name       = "order.create",
            contextualName = "createOrder",
            lowCardinalityKeyValues = {"operation", "create"}
    )
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creazione ordine per customerId={}", request.customerId());

        Timer.Sample timer = orderMetrics.startTimer();
        try {
            Order order = orderMapper.toEntity(request);
            order.setStatus(OrderStatus.PENDING);

            List<OrderItem> items = orderMapper.toItemEntityList(request.items());
            items.forEach(order::addItem);

            BigDecimal total = request.items().stream()
                    .map(i -> i.unitPrice()
                            .multiply(BigDecimal.valueOf(i.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalAmount(total);

            Order saved = orderRepository.save(order);

            // ── Metriche ──────────────────────────────────────────
            orderMetrics.incrementOrdersCreated();

            log.info("Ordine creato id={} customerId={} total={}",
                    saved.getId(), saved.getCustomerId(), saved.getTotalAmount());

            return orderMapper.toResponse(saved);

        } finally {
            orderMetrics.stopTimer(timer);
        }
    }

    // ── getOrderById ──────────────────────────────────────────────
    // Un CUSTOMER può vedere solo i propri ordini.
    // Un ADMIN può vedere qualsiasi ordine.
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("""
        hasRole('ADMIN') or
        @orderSecurityService.isOwner(#orderId, authentication)
        """)
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponse(order);
    }

    // ── getOrders ─────────────────────────────────────────────────
    // Un CUSTOMER vede solo i propri ordini:
    // se customerId non è passato, viene forzato al suo.
    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrders(OrderFilter filter,
                                                Pageable pageable) {
        UUID effectiveCustomerId = SecurityUtils.isAdmin()
                ? filter.customerId()                   // ADMIN: filtro libero
                : SecurityUtils.currentCustomerId();    // CUSTOMER: forzato al suo

        OrderFilter effectiveFilter = OrderFilter.builder()
                .customerId(effectiveCustomerId)
                .status(filter.status())
                .from(filter.from())
                .to(filter.to())
                .build();

        Specification<Order> spec = OrderSpecifications.buildFilter(
                effectiveFilter.customerId(),
                effectiveFilter.status(),
                effectiveFilter.from(),
                effectiveFilter.to());

        return orderRepository
                .findAll(spec, pageable)
                .map(orderMapper::toSummaryResponse);
    }

    // ── updateOrderStatus ── aggiornato ───────────────────────────
    @Override
    @Transactional
    @Observed(
            name       = "order.status.update",
            contextualName = "updateOrderStatus",
            lowCardinalityKeyValues = {"operation", "status_update"}
    )
    public OrderResponse updateOrderStatus(UUID orderId,
                                           UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        stateMachine.validateTransition(order.getStatus(), request.status());

        OrderStatus previous = order.getStatus();
        order.setStatus(request.status());

        // ── Metriche ──────────────────────────────────────────────
        orderMetrics.recordStatusTransition(previous, request.status());

        log.info("Ordine {} transizione {} → {} reason={}",
                orderId, previous, request.status(), request.reason());

        Order updated = orderRepository.save(order);
        return orderMapper.toResponse(updated);
    }

    // ── cancelOrder ── aggiornato ─────────────────────────────────
    @Override
    @Transactional
    public void cancelOrder(UUID orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!stateMachine.canTransition(order.getStatus(), OrderStatus.CANCELLED)) {
            throw new OrderNotCancellableException(orderId, order.getStatus());
        }

        OrderStatus previous = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);

        // ── Metriche ──────────────────────────────────────────────
        orderMetrics.recordStatusTransition(previous, OrderStatus.CANCELLED);
        orderMetrics.incrementOrdersCancelled(previous);

        log.info("Ordine {} cancellato — stato precedente={} reason={}",
                orderId, previous, reason);

        orderRepository.save(order);
    }

    // ── deleteOrder ───────────────────────────────────────────

    @Override
    @Transactional
    public void deleteOrder(UUID orderId) {
        // Verifica esistenza prima di tentare la delete,
        // così possiamo restituire 404 invece di operazione silente.
        if (!orderRepository.existsById(orderId)) {
            throw new OrderNotFoundException(orderId);
        }

        orderRepository.deleteById(orderId);

        log.info("Ordine {} eliminato", orderId);
    }
}