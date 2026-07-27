package com.viktor.booking.api.controller;

import com.viktor.booking.api.dto.AuthResponse;
import com.viktor.booking.api.dto.LoginRequest;
import com.viktor.booking.api.dto.RegisterRequest;
import com.viktor.booking.api.dto.UserResponse;
import com.viktor.booking.application.dto.auth.AuthResult;
import com.viktor.booking.application.service.AuthService;
import com.viktor.booking.domain.model.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResult result = authService.register(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity
                .status(201)
                .body(toResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResult result = authService.login(
                request.getEmail(),
                request.getPassword()
        );

        return ResponseEntity.ok(
                toResponse(result)
        );
    }

    private AuthResponse toResponse(AuthResult result) {
        User user = result.getUser();

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                result.getAccessToken(),
                TOKEN_TYPE,
                userResponse
        );
    }
}
