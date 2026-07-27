package com.viktor.booking.api.security;

import com.viktor.booking.application.exception.InvalidTokenException;
import com.viktor.booking.application.security.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER =
            "Authorization";

    private static final String BEARER_PREFIX =
            "Bearer ";

    private final TokenService tokenService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            TokenService tokenService,
            CustomUserDetailsService userDetailsService
    ) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(AUTHORIZATION_HEADER);

        if (!containsBearerToken(authorizationHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext()
                .getAuthentication() != null) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(
                BEARER_PREFIX.length()
        );

        try {
            authenticateRequest(
                    request,
                    token
            );
        } catch (
                InvalidTokenException |
                UsernameNotFoundException exception
        ) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean containsBearerToken(
            String authorizationHeader
    ) {
        return authorizationHeader != null
                && authorizationHeader.startsWith(
                BEARER_PREFIX
        );
    }

    private void authenticateRequest(
            HttpServletRequest request,
            String token
    ) {
        String email =
                tokenService.extractEmail(token);

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(email);

        if (!(userDetails instanceof AuthenticatedUser authenticatedUser)) {
            return;
        }

        boolean tokenValid =
                tokenService.isTokenValid(
                        token,
                        authenticatedUser.toDomainUser()
                );

        if (!tokenValid) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        authenticatedUser.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource()
                        .buildDetails(request)
        );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }
}
