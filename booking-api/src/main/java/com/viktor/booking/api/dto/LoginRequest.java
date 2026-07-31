package com.viktor.booking.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(
        name = "LoginRequest",
        description = "Credentials used to authenticate an existing user"
)
public class LoginRequest {

    @Schema(
            description = "Registered user email address",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(
            description = "User password",
            example = "StrongPassword123!",
            format = "password",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password must not be blank")
    private String password;

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
