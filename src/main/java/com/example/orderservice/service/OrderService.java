package com.example.orderservice.service;

import com.example.orderservice.dto.request.CreateOrderRequest;
import com.example.orderservice.dto.request.UpdateOrderStatusRequest;
import com.example.orderservice.dto.response.OrderResponse;
import com.example.orderservice.dto.response.OrderSummaryResponse;
import com.example.orderservice.repository.specification.OrderFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(UUID orderId);

    Page<OrderSummaryResponse> getOrders(OrderFilter filter, Pageable pageable);

    OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);

    void cancelOrder(UUID orderId, String reason);

    void deleteOrder(UUID orderId);
}