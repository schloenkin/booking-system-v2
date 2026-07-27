package com.viktor.booking.application.dto.auth;

import com.viktor.booking.domain.model.User;

public class AuthResult {

    private final String accessToken;
    private final User user;

    public AuthResult(
            String accessToken,
            User user
    ) {
        this.accessToken = accessToken;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public User getUser() {
        return user;
    }
}
