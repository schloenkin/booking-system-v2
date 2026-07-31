package com.viktor.booking.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "UserResponse",
        description = "User information returned by the API"
)
public class UserResponse {

    @Schema(
            description = "Unique user identifier",
            example = "7",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    @Schema(
            description = "User email address",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @Schema(
            description = "User authorization role",
            example = "USER",
            allowableValues = {
                    "USER",
                    "ADMIN"
            },
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String role;

    public UserResponse(
            Long id,
            String email,
            String role
    ) {
        this.id = id;
        this.email = email;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}