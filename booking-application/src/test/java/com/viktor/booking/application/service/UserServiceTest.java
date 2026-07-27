package com.viktor.booking.application.service;

import com.viktor.booking.application.repository.UserRepository;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void shouldReturnUserWhenUserExists() {
        User user = new User(
                1L,
                "user@example.com",
                "hashed-password",
                UserRole.USER
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserService userService =
                new UserService(userRepository);

        Optional<User> result =
                userService.getUserById(1L);

        assertThat(result)
                .containsSame(user);

        verify(userRepository)
                .findById(1L);
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        UserService userService =
                new UserService(userRepository);

        Optional<User> result =
                userService.getUserById(99L);

        assertThat(result)
                .isEmpty();

        verify(userRepository)
                .findById(99L);
    }
}
