package com.example.ordering.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDTO(List<CartItemDTO> items, BigDecimal totalAmount) {
}
