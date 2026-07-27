package com.viktor.booking.application.security;

import com.viktor.booking.domain.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedUserContextTest {

    @Test
    void shouldRepresentAuthenticatedUser() {
        AuthenticatedUserContext context =
                new AuthenticatedUserContext(
                        7L,
                        UserRole.USER
                );

        assertThat(context.userId())
                .isEqualTo(7L);

        assertThat(context.role())
                .isEqualTo(UserRole.USER);

        assertThat(context.isAdmin())
                .isFalse();
    }

    @Test
    void shouldRecognizeAdministrator() {
        AuthenticatedUserContext context =
                new AuthenticatedUserContext(
                        9L,
                        UserRole.ADMIN
                );

        assertThat(context.isAdmin())
                .isTrue();
    }

    @Test
    void shouldRejectMissingUserId() {
        assertThatThrownBy(() ->
                new AuthenticatedUserContext(
                        null,
                        UserRole.USER
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Authenticated user id must not be null"
                );
    }

    @Test
    void shouldRejectMissingRole() {
        assertThatThrownBy(() ->
                new AuthenticatedUserContext(
                        7L,
                        null
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Authenticated user role must not be null"
                );
    }
}
