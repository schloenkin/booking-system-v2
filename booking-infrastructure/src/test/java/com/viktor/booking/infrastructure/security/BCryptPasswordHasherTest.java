package com.viktor.booking.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher passwordHasher =
            new BCryptPasswordHasher();

    @Test
    void shouldHashPassword() {
        String passwordHash =
                passwordHasher.hash("raw-password");

        assertThat(passwordHash)
                .isNotBlank();

        assertThat(passwordHash)
                .isNotEqualTo("raw-password");
    }

    @Test
    void shouldMatchCorrectPassword() {
        String passwordHash =
                passwordHasher.hash("correct-password");

        boolean result = passwordHasher.matches(
                "correct-password",
                passwordHash
        );

        assertThat(result)
                .isTrue();
    }

    @Test
    void shouldNotMatchIncorrectPassword() {
        String passwordHash =
                passwordHasher.hash("correct-password");

        boolean result = passwordHasher.matches(
                "incorrect-password",
                passwordHash
        );

        assertThat(result)
                .isFalse();
    }
}
