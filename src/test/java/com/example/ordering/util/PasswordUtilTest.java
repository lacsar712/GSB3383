package com.example.ordering.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {
    @Test
    void shouldHashAndVerifyPassword() {
        String password = "Secret@123";
        String hash = PasswordUtil.hashPassword(password);

        assertThat(hash).contains(":");
        assertThat(PasswordUtil.verifyPassword(password, hash)).isTrue();
        assertThat(PasswordUtil.verifyPassword("wrong-password", hash)).isFalse();
    }

    @Test
    void shouldRejectMalformedHash() {
        assertThat(PasswordUtil.verifyPassword("anything", "broken-hash")).isFalse();
        assertThat(PasswordUtil.verifyPassword("anything", "")).isFalse();
    }
}
