package com.viktor.booking.api.dto;

public class AuthResponse {

    private final String accessToken;
    private final String tokenType;
    private final UserResponse user;

    public AuthResponse(
            String accessToken,
            String tokenType,
            UserResponse user
    ) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public UserResponse getUser() {
        return user;
    }
}
