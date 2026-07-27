package com.viktor.booking.api.controller;

import com.viktor.booking.api.exception.GlobalExceptionHandler;
import com.viktor.booking.application.dto.auth.AuthResult;
import com.viktor.booking.application.exception.InvalidCredentialsException;
import com.viktor.booking.application.exception.UserAlreadyExistsException;
import com.viktor.booking.application.service.AuthService;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController authController =
                new AuthController(authService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void shouldRegisterUserAndReturnToken()
            throws Exception {

        User user = new User(
                1L,
                "user@example.com",
                "hashed-password",
                UserRole.USER
        );

        AuthResult authResult = new AuthResult(
                "jwt-token",
                user
        );

        when(authService.register(
                "user@example.com",
                "strong-password"
        )).thenReturn(authResult);

        String requestBody = """
                {
                  "email": "user@example.com",
                  "password": "strong-password"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.accessToken")
                                .value("jwt-token")
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.user.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.user.email")
                                .value("user@example.com")
                )
                .andExpect(
                        jsonPath("$.user.role")
                                .value("USER")
                );

        verify(authService).register(
                "user@example.com",
                "strong-password"
        );
    }

    @Test
    void shouldLoginUserAndReturnToken()
            throws Exception {

        User user = new User(
                1L,
                "user@example.com",
                "hashed-password",
                UserRole.USER
        );

        AuthResult authResult = new AuthResult(
                "jwt-token",
                user
        );

        when(authService.login(
                "user@example.com",
                "strong-password"
        )).thenReturn(authResult);

        String requestBody = """
                {
                  "email": "user@example.com",
                  "password": "strong-password"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .value("jwt-token")
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.user.email")
                                .value("user@example.com")
                )
                .andExpect(
                        jsonPath("$.user.role")
                                .value("USER")
                );

        verify(authService).login(
                "user@example.com",
                "strong-password"
        );
    }

    @Test
    void shouldReturn400WhenRegisterRequestIsInvalid()
            throws Exception {

        String requestBody = """
                {
                  "email": "user@example.com",
                  "password": "short"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message").value(
                                "Password must contain "
                                        + "between 8 and 72 characters"
                        )
                );
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists()
            throws Exception {

        when(authService.register(
                "existing@example.com",
                "strong-password"
        )).thenThrow(
                new UserAlreadyExistsException(
                        "existing@example.com"
                )
        );

        String requestBody = """
                {
                  "email": "existing@example.com",
                  "password": "strong-password"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message").value(
                                "User already exists with email: "
                                        + "existing@example.com"
                        )
                );
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid()
            throws Exception {

        when(authService.login(
                "user@example.com",
                "wrong-password"
        )).thenThrow(
                new InvalidCredentialsException()
        );

        String requestBody = """
                {
                  "email": "user@example.com",
                  "password": "wrong-password"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message").value(
                                "Invalid email or password"
                        )
                );
    }
}
