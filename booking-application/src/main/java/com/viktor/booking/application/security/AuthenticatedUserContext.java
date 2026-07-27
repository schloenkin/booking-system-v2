package com.viktor.booking.application.security;

import com.viktor.booking.domain.enums.UserRole;

public record AuthenticatedUserContext(
        Long userId,
        UserRole role
) {

    public AuthenticatedUserContext {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "Authenticated user id must not be null"
            );
        }

        if (role == null) {
            throw new IllegalArgumentException(
                    "Authenticated user role must not be null"
            );
        }
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
