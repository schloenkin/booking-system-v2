package com.viktor.booking.api.security;

import com.viktor.booking.application.security.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenService tokenService;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(
            TokenService tokenService,
            CustomUserDetailsService userDetailsService
    ) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(
                        tokenService,
                        userDetailsService
                );

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exceptions ->
                        exceptions
                                .authenticationEntryPoint(
                                        (
                                                request,
                                                response,
                                                exception
                                        ) -> response.sendError(
                                                HttpServletResponse
                                                        .SC_UNAUTHORIZED
                                        )
                                )
                                .accessDeniedHandler(
                                        (
                                                request,
                                                response,
                                                exception
                                        ) -> response.sendError(
                                                HttpServletResponse
                                                        .SC_FORBIDDEN
                                        )
                                )
                )

                .authorizeHttpRequests(authorize ->
                        authorize

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/auth/register",
                                        "/api/auth/login"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/health"
                                ).permitAll()

                                .requestMatchers(
                                        "/",
                                        "/index.html",
                                        "/style.css",
                                        "/app.js",
                                        "/favicon.ico",
                                        "/error"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/services",
                                        "/api/services/*"
                                ).permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/services"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/services/*/activate",
                                        "/api/services/*/deactivate"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        "/api/users/**"
                                ).hasRole("ADMIN")

                                .requestMatchers(
                                        "/api/bookings/**"
                                ).authenticated()

                                .anyRequest().denyAll()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
