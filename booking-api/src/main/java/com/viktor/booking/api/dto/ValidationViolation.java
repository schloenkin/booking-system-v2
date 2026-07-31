package com.viktor.booking.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ValidationViolation",
        description = "Validation error associated with one request field"
)
public record ValidationViolation(

        @Schema(
                description = "Name of the invalid request field",
                example = "password"
        )
        String field,

        @Schema(
                description = "Validation message for the field",
                example = "Password must contain between 8 and 72 characters"
        )
        String message
) {
}
