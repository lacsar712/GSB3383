package com.example.ordering.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderDTO(
        long id,
        long userId,
        String username,
        String status,
        BigDecimal totalAmount,
        String contactName,
        String contactPhone,
        String deliveryAddress,
        String createdAt,
        String updatedAt,
        List<OrderItemDTO> items
) {
}
