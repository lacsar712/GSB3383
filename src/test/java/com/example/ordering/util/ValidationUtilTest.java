package com.example.ordering.util;

import com.example.ordering.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationUtilTest {
    @Test
    void shouldParsePositiveDecimal() {
        BigDecimal value = ValidationUtil.requirePositiveDecimal("价格", "18.50");
        assertThat(value).isEqualByComparingTo("18.50");
    }

    @Test
    void shouldThrowForNegativePrice() {
        assertThatThrownBy(() -> ValidationUtil.requirePositiveDecimal("价格", "-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("必须大于 0");
    }

    @Test
    void shouldThrowForNonIntegerQuantity() {
        assertThatThrownBy(() -> ValidationUtil.parseInt("数量", "abc"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("必须是整数");
    }
}
