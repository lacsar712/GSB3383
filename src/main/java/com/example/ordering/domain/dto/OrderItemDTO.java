package com.example.ordering.domain.dto;

import java.math.BigDecimal;

public record OrderItemDTO(
        long id,
        long menuItemId,
        String itemNameSnapshot,
        BigDecimal unitPriceSnapshot,
        int quantity,
        BigDecimal lineTotal
) {
}
