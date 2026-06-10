package com.example.ordering.domain;

public record User(long id, String username, String passwordHash, Role role, String createdAt) {
}
