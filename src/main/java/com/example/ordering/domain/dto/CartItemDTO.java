package com.example.ordering.domain.dto;

import java.math.BigDecimal;

public record CartItemDTO(
        long id,
        long menuItemId,
        String name,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        String imageUrl,
        boolean isAvailable
) {
}
