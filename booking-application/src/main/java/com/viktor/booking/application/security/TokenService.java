package com.viktor.booking.application.security;

import com.viktor.booking.domain.model.User;

public interface TokenService {

    String generateToken(User user);

    String extractEmail(String token);

    boolean isTokenValid(
            String token,
            User user
    );
}
