package com.viktor.booking.api.security;

import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserTest {

    @Test
    void shouldAdaptDomainUserToSpringSecurityUserDetails() {
        User user = new User(
                7L,
                "user@example.com",
                "hashed-password",
                UserRole.USER
        );

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(user);

        assertThat(authenticatedUser.getUserId())
                .isEqualTo(7L);

        assertThat(authenticatedUser.getUsername())
                .isEqualTo("user@example.com");

        assertThat(authenticatedUser.getPassword())
                .isEqualTo("hashed-password");

        assertThat(authenticatedUser.getRole())
                .isEqualTo(UserRole.USER);
        User restoredUser =
                authenticatedUser.toDomainUser();

        assertThat(restoredUser.getId())
                .isEqualTo(7L);

        assertThat(restoredUser.getEmail())
                .isEqualTo("user@example.com");

        assertThat(restoredUser.getPasswordHash())
                .isEqualTo("hashed-password");

        assertThat(restoredUser.getRole())
                .isEqualTo(UserRole.USER);

        assertThat(authenticatedUser.getAuthorities())
                .extracting(
                        GrantedAuthority::getAuthority
                )
                .containsExactly("ROLE_USER");

        assertThat(authenticatedUser.isAccountNonExpired())
                .isTrue();

        assertThat(authenticatedUser.isAccountNonLocked())
                .isTrue();

        assertThat(authenticatedUser.isCredentialsNonExpired())
                .isTrue();

        assertThat(authenticatedUser.isEnabled())
                .isTrue();
    }

    @Test
    void shouldMapAdminRoleToAdminAuthority() {
        User user = new User(
                8L,
                "admin@example.com",
                "hashed-password",
                UserRole.ADMIN
        );

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(user);

        assertThat(authenticatedUser.getAuthorities())
                .extracting(
                        GrantedAuthority::getAuthority
                )
                .containsExactly("ROLE_ADMIN");
    }
}
