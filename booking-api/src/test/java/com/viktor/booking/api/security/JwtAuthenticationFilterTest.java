package com.viktor.booking.api.security;

import com.viktor.booking.application.exception.InvalidTokenException;
import com.viktor.booking.application.security.TokenService;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenHeaderIsMissing()
            throws Exception {

        JwtAuthenticationFilter filter =
                createFilter();

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        ).isNull();

        verify(tokenService, never())
                .extractEmail(any());

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void shouldContinueWithoutAuthenticationForNonBearerHeader()
            throws Exception {

        JwtAuthenticationFilter filter =
                createFilter();

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Basic credentials"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        ).isNull();

        verify(tokenService, never())
                .extractEmail(any());

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void shouldAuthenticateRequestWithValidToken()
            throws Exception {

        JwtAuthenticationFilter filter =
                createFilter();

        User user = new User(
                1L,
                "user@example.com",
                "hashed-password",
                UserRole.USER
        );

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(user);

        when(tokenService.extractEmail("valid-token"))
                .thenReturn("user@example.com");

        when(userDetailsService.loadUserByUsername(
                "user@example.com"
        )).thenReturn(authenticatedUser);

        when(tokenService.isTokenValid(
                eq("valid-token"),
                argThat(domainUser ->
                        domainUser != null
                                && Long.valueOf(1L)
                                .equals(domainUser.getId())
                                && "user@example.com"
                                .equals(domainUser.getEmail())
                                && "hashed-password"
                                .equals(domainUser.getPasswordHash())
                                && domainUser.getRole()
                                == UserRole.USER
                )
        )).thenReturn(true);

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer valid-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        assertThat(authentication)
                .isNotNull();

        assertThat(authentication.isAuthenticated())
                .isTrue();

        assertThat(authentication.getPrincipal())
                .isSameAs(authenticatedUser);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateRequestWhenTokenIsInvalid()
            throws Exception {

        JwtAuthenticationFilter filter =
                createFilter();

        when(tokenService.extractEmail("invalid-token"))
                .thenThrow(
                        new InvalidTokenException(
                                new IllegalArgumentException(
                                        "Invalid token"
                                )
                        )
                );

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer invalid-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        ).isNull();

        verify(userDetailsService, never())
                .loadUserByUsername(any());

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateRequestWhenTokenDoesNotMatchUser()
            throws Exception {

        JwtAuthenticationFilter filter =
                createFilter();

        User user = new User(
                1L,
                "user@example.com",
                "hashed-password",
                UserRole.USER
        );

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(user);

        when(tokenService.extractEmail("wrong-token"))
                .thenReturn("user@example.com");

        when(userDetailsService.loadUserByUsername(
                "user@example.com"
        )).thenReturn(authenticatedUser);

        when(tokenService.isTokenValid(
                eq("wrong-token"),
                argThat(domainUser ->
                        domainUser != null
                                && Long.valueOf(1L)
                                .equals(domainUser.getId())
                                && "user@example.com"
                                .equals(domainUser.getEmail())
                                && "hashed-password"
                                .equals(domainUser.getPasswordHash())
                                && domainUser.getRole()
                                == UserRole.USER
                )
        )).thenReturn(false);

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                "Authorization",
                "Bearer wrong-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(
                SecurityContextHolder.getContext()
                        .getAuthentication()
        ).isNull();

        verify(filterChain)
                .doFilter(request, response);
    }

    private JwtAuthenticationFilter createFilter() {
        return new JwtAuthenticationFilter(
                tokenService,
                userDetailsService
        );
    }
}
