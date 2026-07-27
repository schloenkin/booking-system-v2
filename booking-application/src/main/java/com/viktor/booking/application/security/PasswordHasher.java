package com.viktor.booking.application.security;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(
            String rawPassword,
            String passwordHash
    );
}
