package com.viktor.booking.infrastructure.security;

import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;
import io.jsonwebtoken.Clock;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private static final String DIFFERENT_SECRET =
            "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    private static final long EXPIRATION_MS =
            60_000L;

    private final User user = new User(
            1L,
            "user@example.com",
            "hashed-password",
            UserRole.USER
    );

    @Test
    void shouldGenerateTokenAndExtractUserEmail() {
        MutableClock clock = createClock();

        JwtTokenService tokenService =
                new JwtTokenService(
                        SECRET,
                        EXPIRATION_MS,
                        clock
                );

        String token =
                tokenService.generateToken(user);

        String extractedEmail =
                tokenService.extractEmail(token);

        assertThat(token)
                .isNotBlank();

        assertThat(extractedEmail)
                .isEqualTo("user@example.com");
    }

    @Test
    void shouldValidateTokenForCorrectUser() {
        MutableClock clock = createClock();

        JwtTokenService tokenService =
                new JwtTokenService(
                        SECRET,
                        EXPIRATION_MS,
                        clock
                );

        String token =
                tokenService.generateToken(user);

        assertThat(
                tokenService.isTokenValid(token, user)
        ).isTrue();
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        MutableClock clock = createClock();

        JwtTokenService tokenService =
                new JwtTokenService(
                        SECRET,
                        EXPIRATION_MS,
                        clock
                );

        String token =
                tokenService.generateToken(user);

        User differentUser = new User(
                2L,
                "different@example.com",
                "hashed-password",
                UserRole.USER
        );

        assertThat(
                tokenService.isTokenValid(
                        token,
                        differentUser
                )
        ).isFalse();
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        MutableClock clock = createClock();

        JwtTokenService tokenCreator =
                new JwtTokenService(
                        SECRET,
                        EXPIRATION_MS,
                        clock
                );

        JwtTokenService tokenValidator =
                new JwtTokenService(
                        DIFFERENT_SECRET,
                        EXPIRATION_MS,
                        clock
                );

        String token =
                tokenCreator.generateToken(user);

        assertThat(
                tokenValidator.isTokenValid(token, user)
        ).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        MutableClock clock = createClock();

        JwtTokenService tokenService =
                new JwtTokenService(
                        SECRET,
                        EXPIRATION_MS,
                        clock
                );

        String token =
                tokenService.generateToken(user);

        clock.advanceMillis(
                EXPIRATION_MS + 1
        );

        assertThat(
                tokenService.isTokenValid(token, user)
        ).isFalse();
    }

    @Test
    void shouldRejectNonPositiveExpiration() {
        assertThatThrownBy(() ->
                new JwtTokenService(
                        SECRET,
                        0L,
                        createClock()
                )
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "JWT expiration must be greater than zero"
                );
    }

    private MutableClock createClock() {
        return new MutableClock(
                Date.from(
                        Instant.parse(
                                "2026-07-27T10:00:00Z"
                        )
                )
        );
    }

    private static class MutableClock
            implements Clock {

        private Date currentTime;

        private MutableClock(Date currentTime) {
            this.currentTime =
                    new Date(currentTime.getTime());
        }

        @Override
        public Date now() {
            return new Date(
                    currentTime.getTime()
            );
        }

        private void advanceMillis(long milliseconds) {
            currentTime = new Date(
                    currentTime.getTime() + milliseconds
            );
        }
    }
}
