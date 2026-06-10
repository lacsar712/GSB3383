package com.example.ordering.service;

import com.example.ordering.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {
    @Test
    void shouldAllowValidTransitions() {
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.CANCELED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.COMPLETED)).isTrue();
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELED)).isTrue();
    }

    @Test
    void shouldRejectInvalidTransitions() {
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.COMPLETED)).isFalse();
        assertThat(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.CANCELED)).isFalse();
        assertThat(OrderStatus.CANCELED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.PLACED)).isFalse();
    }
}
