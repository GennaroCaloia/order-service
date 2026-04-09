package com.example.orderservice.controller;

import com.example.orderservice.domain.enums.OrderStatus;
import com.example.orderservice.support.AbstractIntegrationTest;
import com.example.orderservice.support.OrderServiceFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("OrderController — Integration Tests")
class OrderControllerIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    // ── POST /api/v1/orders ───────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/orders")
    class CreateOrder {

        @Test
        @DisplayName("201 — crea ordine valido e restituisce Location header")
        void shouldCreateOrderAndReturnLocation() throws Exception {
            Map<String, Object> request = Map.of(
                    "customerId", OrderServiceFixtures.CUSTOMER_ID.toString(),
                    "items", List.of(Map.of(
                            "productId",   OrderServiceFixtures.PRODUCT_ID.toString(),
                            "productName", "Laptop Pro 15\"",
                            "quantity",    1,
                            "unitPrice",   "1199.99"
                    ))
            );

            mockMvc.perform(post("/api/v1/orders")
                            .header("Authorization", customerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalAmount").value(1199.99))
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].productName")
                            .value("Laptop Pro 15\""));
        }

        @Test
        @DisplayName("400 — lista items vuota")
        void shouldReturn400ForEmptyItems() throws Exception {
            Map<String, Object> request = Map.of(
                    "customerId", OrderServiceFixtures.CUSTOMER_ID.toString(),
                    "items",      List.of()
            );

            mockMvc.perform(post("/api/v1/orders")
                            .header("Authorization", customerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("items"))
                    .andExpect(jsonPath("$.traceId").doesNotExist()); // traceId null → NON_NULL esclude
        }

        @Test
        @DisplayName("400 — customerId mancante")
        void shouldReturn400ForMissingCustomerId() throws Exception {
            Map<String, Object> request = Map.of(
                    "items", List.of(Map.of(
                            "productId",   UUID.randomUUID().toString(),
                            "productName", "Mouse",
                            "quantity",    1,
                            "unitPrice",   "29.99"
                    ))
            );

            mockMvc.perform(post("/api/v1/orders")
                            .header("Authorization", adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field=='customerId')]")
                            .exists());
        }

        @Test
        @DisplayName("401 — nessun token")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /api/v1/orders/{id} ───────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    @Sql(scripts = "/sql/seed-orders.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    class GetOrderById {

        @Test
        @DisplayName("200 — CUSTOMER vede il proprio ordine")
        void shouldReturnOrderForOwner() throws Exception {
            mockMvc.perform(get("/api/v1/orders/{id}",
                            OrderServiceFixtures.ORDER_ID)
                            .header("Authorization", customerToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id")
                            .value(OrderServiceFixtures.ORDER_ID.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.items", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("200 — ADMIN vede qualsiasi ordine")
        void shouldReturnOrderForAdmin() throws Exception {
            mockMvc.perform(get("/api/v1/orders/{id}",
                            OrderServiceFixtures.ORDER_ID)
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("403 — CUSTOMER non può vedere l'ordine di un altro customer")
        void shouldReturn403ForOtherCustomerOrder() throws Exception {
            // Crea un token per un customer diverso
            String otherCustomerToken = bearerToken(
                    com.example.orderservice.support.JwtTestUtils.customerToken(
                            UUID.fromString("a0000000-0000-0000-0000-000000000002")));

            mockMvc.perform(get("/api/v1/orders/{id}",
                            OrderServiceFixtures.ORDER_ID)
                            .header("Authorization", otherCustomerToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("404 — ordine non trovato")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/orders/{id}", UUID.randomUUID())
                            .header("Authorization", adminToken()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not found"))
                    .andExpect(jsonPath("$.path").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    // ── GET /api/v1/orders ────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/orders")
    @Sql(scripts = "/sql/seed-orders.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    class GetOrders {

        @Test
        @DisplayName("200 — lista paginata con metadata corretti")
        void shouldReturnPagedOrders() throws Exception {
            mockMvc.perform(get("/api/v1/orders")
                            .param("page", "0")
                            .param("size", "10")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(greaterThan(0))))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10))
                    .andExpect(jsonPath("$.totalElements").isNumber());
        }

        @Test
        @DisplayName("200 — CUSTOMER vede solo i propri ordini")
        void shouldReturnOnlyOwnOrdersForCustomer() throws Exception {
            mockMvc.perform(get("/api/v1/orders")
                            .header("Authorization", customerToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].customerId",
                            everyItem(is(OrderServiceFixtures.CUSTOMER_ID.toString()))));
        }

        @Test
        @DisplayName("200 — filtra per status")
        void shouldFilterByStatus() throws Exception {
            mockMvc.perform(get("/api/v1/orders")
                            .param("status", "PENDING")
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].status",
                            everyItem(is("PENDING"))));
        }
    }

    // ── PATCH /api/v1/orders/{id}/status ─────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/orders/{id}/status")
    @Sql(scripts = "/sql/seed-orders.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    class UpdateOrderStatus {

        @Test
        @DisplayName("200 — PENDING → CONFIRMED")
        void shouldTransitionPendingToConfirmed() throws Exception {
            Map<String, String> request = Map.of(
                    "status", "CONFIRMED",
                    "reason", "Pagamento ricevuto");

            mockMvc.perform(patch("/api/v1/orders/{id}/status",
                            OrderServiceFixtures.ORDER_ID)
                            .header("Authorization", adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("409 — transizione illegale PENDING → DELIVERED")
        void shouldReturn409ForIllegalTransition() throws Exception {
            Map<String, String> request = Map.of("status", "DELIVERED");

            mockMvc.perform(patch("/api/v1/orders/{id}/status",
                            OrderServiceFixtures.ORDER_ID)
                            .header("Authorization", adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value("State conflict"))
                    .andExpect(jsonPath("$.message",
                            containsString("PENDING")))
                    .andExpect(jsonPath("$.message",
                            containsString("DELIVERED")));
        }

        @Test
        @DisplayName("400 — status mancante nel body")
        void shouldReturn400ForMissingStatus() throws Exception {
            mockMvc.perform(patch("/api/v1/orders/{id}/status",
                            OrderServiceFixtures.ORDER_ID)
                            .header("Authorization", adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ── DELETE /api/v1/orders/{id} ────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/orders/{id}")
    @Sql(scripts = "/sql/seed-orders.sql",
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/sql/cleanup.sql",
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    class CancelOrder {

        @Test
        @DisplayName("204 — cancella ordine PENDING")
        void shouldCancelPendingOrder() throws Exception {
            mockMvc.perform(delete("/api/v1/orders/{id}",
                            OrderServiceFixtures.ORDER_ID)
                            .header("Authorization", adminToken()))
                    .andExpect(status().isNoContent());

            // Verifica che lo stato sia effettivamente cambiato
            mockMvc.perform(get("/api/v1/orders/{id}",
                            OrderServiceFixtures.ORDER_ID)
                            .header("Authorization", adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("422 — ordine SHIPPED non cancellabile")
        void shouldReturn422ForShippedOrder() throws Exception {
            // ord_3 dal seed è in stato SHIPPED (Bob's order)
            String shippedOrderId = "c0000000-0000-0000-0000-000000000003";

            mockMvc.perform(delete("/api/v1/orders/{id}", shippedOrderId)
                            .header("Authorization", adminToken()))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error").value("Unprocessable"));
        }

        @Test
        @DisplayName("404 — ordine inesistente")
        void shouldReturn404ForNonExistentOrder() throws Exception {
            mockMvc.perform(delete("/api/v1/orders/{id}", UUID.randomUUID())
                            .header("Authorization", adminToken()))
                    .andExpect(status().isNotFound());
        }
    }

    // ── Flusso completo end-to-end ────────────────────────────

    @Nested
    @DisplayName("Flusso end-to-end")
    class EndToEnd {

        @Test
        @DisplayName("Crea → Conferma → Spedisce → Consegna")
        void shouldCompleteFullOrderLifecycle() throws Exception {
            // 1. Crea ordine
            Map<String, Object> createRequest = Map.of(
                    "customerId", OrderServiceFixtures.CUSTOMER_ID.toString(),
                    "items", List.of(Map.of(
                            "productId",   OrderServiceFixtures.PRODUCT_ID.toString(),
                            "productName", "Monitor 4K",
                            "quantity",    2,
                            "unitPrice",   "499.99"
                    ))
            );

            String location = mockMvc.perform(post("/api/v1/orders")
                            .header("Authorization", customerToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalAmount").value(999.98))
                    .andReturn()
                    .getResponse()
                    .getHeader("Location");

            // Estrai l'ID dall'header Location
            String orderId = location.substring(location.lastIndexOf('/') + 1);

            // 2. PENDING → CONFIRMED
            mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                            .header("Authorization", adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("status", "CONFIRMED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));

            // 3. CONFIRMED → SHIPPED
            mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                            .header("Authorization", adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("status", "SHIPPED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SHIPPED"));

            // 4. SHIPPED → DELIVERED
            mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                            .header("Authorization", adminToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("status", "DELIVERED"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DELIVERED"));

            // 5. Verifica stato terminale — cancellazione non consentita
            mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
                            .header("Authorization", adminToken()))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}