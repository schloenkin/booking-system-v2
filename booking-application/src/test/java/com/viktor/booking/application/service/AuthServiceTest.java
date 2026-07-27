package com.viktor.booking.application.service;

import com.viktor.booking.application.dto.auth.AuthResult;
import com.viktor.booking.application.exception.InvalidCredentialsException;
import com.viktor.booking.application.exception.UserAlreadyExistsException;
import com.viktor.booking.application.repository.UserRepository;
import com.viktor.booking.application.security.PasswordHasher;
import com.viktor.booking.application.security.TokenService;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenService tokenService;

    @Test
    void shouldRegisterUserWithNormalizedEmailAndReturnToken() {
        AuthService authService = new AuthService(
                userRepository,
                passwordHasher,
                tokenService
        );

        when(userRepository.existsByEmail("user@example.com"))
                .thenReturn(false);

        when(passwordHasher.hash("raw-password"))
                .thenReturn("hashed-password");

        User savedUser = new User(
                1L,
                "user@example.com",
                "hashed-password",
                UserRole.USER
        );

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        when(tokenService.generateToken(savedUser))
                .thenReturn("jwt-token");

        AuthResult result = authService.register(
                "  User@Example.COM  ",
                "raw-password"
        );

        verify(userRepository)
                .existsByEmail("user@example.com");

        verify(passwordHasher)
                .hash("raw-password");

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository)
                .save(userCaptor.capture());

        User userPassedToRepository =
                userCaptor.getValue();

        assertThat(userPassedToRepository.getId())
                .isNull();

        assertThat(userPassedToRepository.getEmail())
                .isEqualTo("user@example.com");

        assertThat(userPassedToRepository.getPasswordHash())
                .isEqualTo("hashed-password");

        assertThat(userPassedToRepository.getRole())
                .isEqualTo(UserRole.USER);

        verify(tokenService)
                .generateToken(savedUser);

        assertThat(result.getAccessToken())
                .isEqualTo("jwt-token");

        assertThat(result.getUser())
                .isSameAs(savedUser);
    }

    @Test
    void shouldRejectRegistrationWhenEmailAlreadyExists() {
        AuthService authService = new AuthService(
                userRepository,
                passwordHasher,
                tokenService
        );

        when(userRepository.existsByEmail("existing@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                " Existing@Example.COM ",
                "raw-password"
        ))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessage(
                        "User already exists with email: existing@example.com"
                );

        verify(userRepository)
                .existsByEmail("existing@example.com");

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(
                passwordHasher,
                tokenService
        );
    }

    @Test
    void shouldLoginWithValidCredentialsAndReturnToken() {
        AuthService authService = new AuthService(
                userRepository,
                passwordHasher,
                tokenService
        );

        User user = new User(
                1L,
                "user@example.com",
                "hashed-password",
                UserRole.USER
        );

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordHasher.matches(
                "correct-password",
                "hashed-password"
        ))
                .thenReturn(true);

        when(tokenService.generateToken(user))
                .thenReturn("jwt-token");

        AuthResult result = authService.login(
                " User@Example.COM ",
                "correct-password"
        );

        verify(userRepository)
                .findByEmail("user@example.com");

        verify(passwordHasher)
                .matches(
                        "correct-password",
                        "hashed-password"
                );

        verify(tokenService)
                .generateToken(user);

        assertThat(result.getAccessToken())
                .isEqualTo("jwt-token");

        assertThat(result.getUser())
                .isSameAs(user);
    }

    @Test
    void shouldRejectLoginWhenUserDoesNotExist() {
        AuthService authService = new AuthService(
                userRepository,
                passwordHasher,
                tokenService
        );

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                " Missing@Example.COM ",
                "password"
        ))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository)
                .findByEmail("missing@example.com");

        verifyNoInteractions(
                passwordHasher,
                tokenService
        );
    }

    @Test
    void shouldRejectLoginWhenPasswordIsIncorrect() {
        AuthService authService = new AuthService(
                userRepository,
                passwordHasher,
                tokenService
        );

        User user = new User(
                1L,
                "user@example.com",
                "hashed-password",
                UserRole.USER
        );

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordHasher.matches(
                "incorrect-password",
                "hashed-password"
        ))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                "user@example.com",
                "incorrect-password"
        ))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(passwordHasher)
                .matches(
                        "incorrect-password",
                        "hashed-password"
                );

        verify(tokenService, never())
                .generateToken(any(User.class));
    }
}
