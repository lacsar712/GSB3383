package com.example.ordering.util;

import com.example.ordering.exception.ApiException;

import java.math.BigDecimal;

public final class ValidationUtil {
    private ValidationUtil() {
    }

    public static String requireNonBlank(final String fieldName, final String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(400, "VALIDATION_ERROR", fieldName + " 不能为空");
        }
        return value.trim();
    }

    public static String requireLength(final String fieldName, final String value, final int min, final int max) {
        String normalized = requireNonBlank(fieldName, value);
        int len = normalized.length();
        if (len < min || len > max) {
            throw new ApiException(400, "VALIDATION_ERROR",
                    fieldName + " 长度必须在 " + min + " 到 " + max + " 之间");
        }
        return normalized;
    }

    public static int parseInt(final String fieldName, final String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "VALIDATION_ERROR", fieldName + " 必须是整数");
        }
    }

    public static int requirePositiveInt(final String fieldName, final int value) {
        if (value <= 0) {
            throw new ApiException(400, "VALIDATION_ERROR", fieldName + " 必须是正整数");
        }
        return value;
    }

    public static BigDecimal requirePositiveDecimal(final String fieldName, final String raw) {
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(400, "VALIDATION_ERROR", fieldName + " 必须大于 0");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new ApiException(400, "VALIDATION_ERROR", fieldName + " 必须是数字");
        }
    }
}
