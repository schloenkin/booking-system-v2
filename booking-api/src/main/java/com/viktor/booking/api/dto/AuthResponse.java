package com.viktor.booking.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "AuthResponse",
        description = "Authentication result containing a JWT and user details"
)
public class AuthResponse {

    @Schema(
            description = "JWT access token",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final String accessToken;

    @Schema(
            description = "Authentication scheme used with the access token",
            example = "Bearer",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final String tokenType;

    @Schema(
            description = "Authenticated user information",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
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
