package com.viktor.booking.api.security;

import com.viktor.booking.application.repository.UserRepository;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void shouldLoadUserByNormalizedEmail() {
        User user = new User(
                1L,
                "user@example.com",
                "hashed-password",
                UserRole.USER
        );

        when(userRepository.findByEmail(
                "user@example.com"
        )).thenReturn(Optional.of(user));

        CustomUserDetailsService userDetailsService =
                new CustomUserDetailsService(
                        userRepository
                );

        UserDetails result =
                userDetailsService.loadUserByUsername(
                        "  User@Example.COM  "
                );

        assertThat(result)
                .isInstanceOf(
                        AuthenticatedUser.class
                );

        assertThat(result.getUsername())
                .isEqualTo("user@example.com");

        verify(userRepository)
                .findByEmail("user@example.com");
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        when(userRepository.findByEmail(
                "missing@example.com"
        )).thenReturn(Optional.empty());

        CustomUserDetailsService userDetailsService =
                new CustomUserDetailsService(
                        userRepository
                );

        assertThatThrownBy(() ->
                userDetailsService.loadUserByUsername(
                        "Missing@Example.COM"
                )
        )
                .isInstanceOf(
                        UsernameNotFoundException.class
                )
                .hasMessage("User not found");

        verify(userRepository)
                .findByEmail("missing@example.com");
    }
}
