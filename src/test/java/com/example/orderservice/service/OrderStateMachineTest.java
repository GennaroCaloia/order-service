package com.example.orderservice.service;

import com.example.orderservice.domain.enums.OrderStatus;
import com.example.orderservice.exception.IllegalOrderStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrderStateMachine")
class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    // ── Transizioni valide ────────────────────────────────────

    @Nested
    @DisplayName("Transizioni consentite")
    class ValidTransitions {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "PENDING,   CONFIRMED",
                "PENDING,   CANCELLED",
                "CONFIRMED, SHIPPED",
                "CONFIRMED, CANCELLED",
                "SHIPPED,   DELIVERED"
        })
        void shouldAllowValidTransition(OrderStatus from, OrderStatus to) {
            assertThatNoException()
                    .isThrownBy(() -> stateMachine.validateTransition(from, to));
        }

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "PENDING,   CONFIRMED",
                "PENDING,   CANCELLED",
                "CONFIRMED, SHIPPED",
                "CONFIRMED, CANCELLED",
                "SHIPPED,   DELIVERED"
        })
        void canTransitionShouldReturnTrue(OrderStatus from, OrderStatus to) {
            assertThat(stateMachine.canTransition(from, to)).isTrue();
        }
    }

    // ── Transizioni illegali ──────────────────────────────────

    @Nested
    @DisplayName("Transizioni non consentite")
    class InvalidTransitions {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "PENDING,   SHIPPED",
                "PENDING,   DELIVERED",
                "CONFIRMED, PENDING",
                "CONFIRMED, DELIVERED",
                "SHIPPED,   PENDING",
                "SHIPPED,   CONFIRMED",
                "SHIPPED,   CANCELLED",
                "DELIVERED, CANCELLED",
                "DELIVERED, PENDING",
                "DELIVERED, CONFIRMED",
                "DELIVERED, SHIPPED",
                "CANCELLED, PENDING",
                "CANCELLED, CONFIRMED",
                "CANCELLED, SHIPPED",
                "CANCELLED, DELIVERED"
        })
        void shouldRejectInvalidTransition(OrderStatus from, OrderStatus to) {
            assertThatThrownBy(() -> stateMachine.validateTransition(from, to))
                    .isInstanceOf(IllegalOrderStateException.class)
                    .hasMessageContaining(from.name())
                    .hasMessageContaining(to.name());
        }

        @Test
        @DisplayName("Gli stati terminali non hanno transizioni consentite")
        void terminalStatesShouldHaveNoAllowedTransitions() {
            assertThat(stateMachine.allowedTransitions(OrderStatus.DELIVERED)).isEmpty();
            assertThat(stateMachine.allowedTransitions(OrderStatus.CANCELLED)).isEmpty();
        }
    }

    // ── allowedTransitions ────────────────────────────────────

    @Nested
    @DisplayName("allowedTransitions")
    class AllowedTransitions {

        @Test
        void pendingShouldAllowConfirmedAndCancelled() {
            Set<OrderStatus> allowed =
                    stateMachine.allowedTransitions(OrderStatus.PENDING);
            assertThat(allowed)
                    .containsExactlyInAnyOrder(
                            OrderStatus.CONFIRMED,
                            OrderStatus.CANCELLED);
        }

        @Test
        void confirmedShouldAllowShippedAndCancelled() {
            Set<OrderStatus> allowed =
                    stateMachine.allowedTransitions(OrderStatus.CONFIRMED);
            assertThat(allowed)
                    .containsExactlyInAnyOrder(
                            OrderStatus.SHIPPED,
                            OrderStatus.CANCELLED);
        }

        @Test
        void shippedShouldAllowOnlyDelivered() {
            Set<OrderStatus> allowed =
                    stateMachine.allowedTransitions(OrderStatus.SHIPPED);
            assertThat(allowed)
                    .containsExactly(OrderStatus.DELIVERED);
        }
    }
}