package com.example.ordering.domain;

public enum Role {
    USER,
    ADMIN;

    public static Role fromString(final String raw) {
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(raw)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + raw);
    }
}
