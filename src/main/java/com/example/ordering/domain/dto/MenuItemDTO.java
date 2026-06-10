package com.example.ordering.domain.dto;

import java.math.BigDecimal;

public record MenuItemDTO(
        long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean isAvailable
) {
}
