package com.example.orderservice.service.impl;

import com.example.orderservice.domain.entity.Order;
import com.example.orderservice.domain.enums.OrderStatus;
import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.UpdateOrderStatusRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.OrderSummaryResponse;
import com.example.orderservice.exception.IllegalOrderStateException;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.exception.OrderNotCancellableException;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.specification.OrderFilter;
import com.example.orderservice.service.OrderStateMachine;
import com.example.orderservice.support.OrderServiceFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.orderservice.support.OrderServiceFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl")
class OrderServiceImplTest {

    @Mock private OrderRepository   orderRepository;
    @Mock private OrderMapper       orderMapper;
    @Mock private OrderStateMachine stateMachine;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ── createOrder ───────────────────────────────────────────

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("Crea ordine con status PENDING e total calcolato correttamente")
        void shouldCreateOrderWithPendingStatusAndCorrectTotal() {
            // Arrange
            CreateOrderRequest request = createOrderRequest();
            Order mappedOrder = pendingOrder();
            mappedOrder.getItems().clear();         // il mapper non aggiunge items
            Order savedOrder  = pendingOrder();

            given(orderMapper.toEntity(request)).willReturn(mappedOrder);
            given(orderMapper.toItemEntityList(request.items()))
                    .willReturn(List.of(orderItem(mappedOrder)));
            given(orderRepository.save(any(Order.class))).willReturn(savedOrder);
            given(orderMapper.toResponse(savedOrder)).willReturn(
                    OrderResponse.builder()
                            .id(savedOrder.getId())
                            .status(OrderStatus.PENDING)
                            .totalAmount(savedOrder.getTotalAmount())
                            .build());

            // Act
            OrderResponse response = orderService.createOrder(request);

            // Assert
            assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(response.totalAmount())
                    .isEqualByComparingTo("149.98");

            // Verifica che save sia stato chiamato con il totale corretto
            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            then(orderRepository).should().save(captor.capture());
            assertThat(captor.getValue().getTotalAmount())
                    .isEqualByComparingTo("149.98");
        }

        @Test
        @DisplayName("Aggiunge gli item tramite addItem() mantenendo la relazione bidirezionale")
        void shouldAddItemsViaBidirectionalHelper() {
            CreateOrderRequest request = createOrderRequest();
            Order mappedOrder = Order.builder()
                    .id(ORDER_ID)
                    .customerId(CUSTOMER_ID)
                    .status(OrderStatus.PENDING)
                    .build();

            given(orderMapper.toEntity(request)).willReturn(mappedOrder);
            given(orderMapper.toItemEntityList(request.items()))
                    .willReturn(List.of(orderItem(mappedOrder)));
            given(orderRepository.save(any())).willReturn(pendingOrder());
            given(orderMapper.toResponse(any())).willReturn(
                    OrderResponse.builder().build());

            orderService.createOrder(request);

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            then(orderRepository).should().save(captor.capture());

            // Ogni item deve avere il riferimento all'ordine impostato
            captor.getValue().getItems()
                    .forEach(item ->
                            assertThat(item.getOrder()).isNotNull());
        }
    }

    // ── getOrderById ──────────────────────────────────────────

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("Restituisce l'ordine quando trovato")
        void shouldReturnOrderWhenFound() {
            Order order = pendingOrder();
            OrderResponse expected = OrderResponse.builder()
                    .id(ORDER_ID).build();

            given(orderRepository.findByIdWithItems(ORDER_ID))
                    .willReturn(Optional.of(order));
            given(orderMapper.toResponse(order)).willReturn(expected);

            OrderResponse result = orderService.getOrderById(ORDER_ID);

            assertThat(result.id()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("Lancia OrderNotFoundException quando non trovato")
        void shouldThrowWhenNotFound() {
            given(orderRepository.findByIdWithItems(ORDER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining(ORDER_ID.toString());
        }
    }

    // ── getOrders ─────────────────────────────────────────────

    @Nested
    @DisplayName("getOrders")
    class GetOrders {

        @Test
        @DisplayName("Delega la paginazione al repository con la Specification corretta")
        void shouldDelegateToRepositoryWithSpecification() {
            OrderFilter filter = OrderFilter.builder()
                    .customerId(CUSTOMER_ID)
                    .status(OrderStatus.PENDING)
                    .build();
            PageRequest pageable = PageRequest.of(0, 10);

            given(orderRepository.findAll(
                    any(Specification.class), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(pendingOrder())));
            given(orderMapper.toSummaryResponse(any(Order.class)))
                    .willReturn(OrderSummaryResponse.builder()
                            .id(ORDER_ID)
                            .status(OrderStatus.PENDING)
                            .build());

            var result = orderService.getOrders(filter, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).status())
                    .isEqualTo(OrderStatus.PENDING);
        }
    }

    // ── updateOrderStatus ─────────────────────────────────────

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatus {

        @Test
        @DisplayName("Aggiorna lo stato quando la transizione è valida")
        void shouldUpdateStatusOnValidTransition() {
            Order order = pendingOrder();
            UpdateOrderStatusRequest request =
                    updateStatusRequest(OrderStatus.CONFIRMED);

            given(orderRepository.findById(ORDER_ID))
                    .willReturn(Optional.of(order));
            // stateMachine.validateTransition non lancia → void mock non necessario
            given(orderRepository.save(order)).willReturn(order);
            given(orderMapper.toResponse(order))
                    .willReturn(OrderResponse.builder()
                            .status(OrderStatus.CONFIRMED).build());

            OrderResponse response =
                    orderService.updateOrderStatus(ORDER_ID, request);

            assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

            then(stateMachine).should()
                    .validateTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Propaga IllegalOrderStateException dalla state machine")
        void shouldPropagateIllegalStateException() {
            Order order = orderWithStatus(OrderStatus.DELIVERED);
            UpdateOrderStatusRequest request =
                    updateStatusRequest(OrderStatus.CANCELLED);

            given(orderRepository.findById(ORDER_ID))
                    .willReturn(Optional.of(order));
            willThrow(new IllegalOrderStateException(
                    OrderStatus.DELIVERED, OrderStatus.CANCELLED))
                    .given(stateMachine)
                    .validateTransition(OrderStatus.DELIVERED, OrderStatus.CANCELLED);

            assertThatThrownBy(() ->
                    orderService.updateOrderStatus(ORDER_ID, request))
                    .isInstanceOf(IllegalOrderStateException.class);

            // Il repository non deve essere chiamato dopo l'eccezione
            then(orderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Lancia OrderNotFoundException se l'ordine non esiste")
        void shouldThrowWhenOrderNotFound() {
            given(orderRepository.findById(any()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    orderService.updateOrderStatus(
                            UUID.randomUUID(),
                            updateStatusRequest(OrderStatus.CONFIRMED)))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    // ── cancelOrder ───────────────────────────────────────────

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("Cancella un ordine PENDING")
        void shouldCancelPendingOrder() {
            Order order = pendingOrder();

            given(orderRepository.findById(ORDER_ID))
                    .willReturn(Optional.of(order));
            given(stateMachine.canTransition(
                    OrderStatus.PENDING, OrderStatus.CANCELLED))
                    .willReturn(true);
            given(orderRepository.save(order)).willReturn(order);

            orderService.cancelOrder(ORDER_ID, "test");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            then(orderRepository).should().save(order);
        }

        @Test
        @DisplayName("Cancella un ordine CONFIRMED")
        void shouldCancelConfirmedOrder() {
            Order order = orderWithStatus(OrderStatus.CONFIRMED);

            given(orderRepository.findById(ORDER_ID))
                    .willReturn(Optional.of(order));
            given(stateMachine.canTransition(
                    OrderStatus.CONFIRMED, OrderStatus.CANCELLED))
                    .willReturn(true);
            given(orderRepository.save(order)).willReturn(order);

            orderService.cancelOrder(ORDER_ID, "cambio idea");

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("Lancia OrderNotCancellableException per ordine SHIPPED")
        void shouldThrowForShippedOrder() {
            Order order = orderWithStatus(OrderStatus.SHIPPED);

            given(orderRepository.findById(ORDER_ID))
                    .willReturn(Optional.of(order));
            given(stateMachine.canTransition(
                    OrderStatus.SHIPPED, OrderStatus.CANCELLED))
                    .willReturn(false);

            assertThatThrownBy(() ->
                    orderService.cancelOrder(ORDER_ID, "troppo tardi"))
                    .isInstanceOf(OrderNotCancellableException.class)
                    .hasMessageContaining(OrderStatus.SHIPPED.name());

            then(orderRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("Lancia OrderNotCancellableException per ordine DELIVERED")
        void shouldThrowForDeliveredOrder() {
            Order order = orderWithStatus(OrderStatus.DELIVERED);

            given(orderRepository.findById(ORDER_ID))
                    .willReturn(Optional.of(order));
            given(stateMachine.canTransition(
                    OrderStatus.DELIVERED, OrderStatus.CANCELLED))
                    .willReturn(false);

            assertThatThrownBy(() ->
                    orderService.cancelOrder(ORDER_ID, "nope"))
                    .isInstanceOf(OrderNotCancellableException.class);
        }
    }

    // ── deleteOrder ───────────────────────────────────────────

    @Nested
    @DisplayName("deleteOrder")
    class DeleteOrder {

        @Test
        @DisplayName("Elimina l'ordine quando esiste")
        void shouldDeleteWhenExists() {
            given(orderRepository.existsById(ORDER_ID)).willReturn(true);

            orderService.deleteOrder(ORDER_ID);

            then(orderRepository).should().deleteById(ORDER_ID);
        }

        @Test
        @DisplayName("Lancia OrderNotFoundException se non esiste")
        void shouldThrowWhenNotExists() {
            given(orderRepository.existsById(ORDER_ID)).willReturn(false);

            assertThatThrownBy(() -> orderService.deleteOrder(ORDER_ID))
                    .isInstanceOf(OrderNotFoundException.class);

            then(orderRepository).should(never()).deleteById(any());
        }
    }
}