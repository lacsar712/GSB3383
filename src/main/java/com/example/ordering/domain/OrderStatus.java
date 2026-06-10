package com.example.ordering.domain;

public enum OrderStatus {
    PLACED,
    CONFIRMED,
    COMPLETED,
    CANCELED;

    public boolean canTransitionTo(final OrderStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case PLACED -> target == CONFIRMED || target == CANCELED;
            case CONFIRMED -> target == COMPLETED || target == CANCELED;
            case COMPLETED, CANCELED -> false;
        };
    }

    public static OrderStatus fromString(final String raw) {
        for (OrderStatus status : values()) {
            if (status.name().equalsIgnoreCase(raw)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status: " + raw);
    }
}
