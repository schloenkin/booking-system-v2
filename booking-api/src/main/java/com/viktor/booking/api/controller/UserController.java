package com.viktor.booking.api.controller;

import com.viktor.booking.api.config.OpenApiConfig;
import com.viktor.booking.api.dto.ErrorResponse;
import com.viktor.booking.api.dto.UserResponse;
import com.viktor.booking.application.exception.UserNotFoundException;
import com.viktor.booking.application.service.UserService;
import com.viktor.booking.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
        name = "Users",
        description = "Administrative operations for accessing user information"
)
@SecurityRequirement(
        name = OpenApiConfig.SECURITY_SCHEME_NAME
)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Get a user",
            description = """
                    Returns a user by identifier.

                    Only ADMIN accounts can access this endpoint.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = UserResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ADMIN role is required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User was not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping("/api/users/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(
                    description = "User identifier",
                    example = "7",
                    required = true
            )
            @PathVariable("id")
            Long id
    ) {
        User user =
                userService.getUserById(id)
                        .orElseThrow(
                                () -> new UserNotFoundException(id)
                        );

        return ResponseEntity.ok(
                toResponse(user)
        );
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}