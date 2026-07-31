package com.viktor.booking.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(
        name = "ErrorResponse",
        description = "Unified error response returned by the API"
)
public class ErrorResponse {

    @Schema(
            description = "Time when the error response was created",
            example = "2026-07-31T12:00:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final Instant timestamp;

    @Schema(
            description = "HTTP status code",
            example = "400",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final int status;

    @Schema(
            description = "Standard HTTP status description",
            example = "Bad Request",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final String error;

    @Schema(
            description = "Stable machine-readable API error code",
            example = "VALIDATION_FAILED",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final ApiErrorCode code;

    @Schema(
            description = "Human-readable explanation of the error",
            example = "Request validation failed",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final String message;

    @Schema(
            description = "Request path that produced the error",
            example = "/api/auth/register",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final String path;

    @Schema(
            description = "Individual field validation errors; empty for non-validation errors",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private final List<ValidationViolation> violations;

    public ErrorResponse(
            int status,
            String error,
            ApiErrorCode code,
            String message,
            String path
    ) {
        this(
                status,
                error,
                code,
                message,
                path,
                List.of()
        );
    }

    public ErrorResponse(
            int status,
            String error,
            ApiErrorCode code,
            String message,
            String path,
            List<ValidationViolation> violations
    ) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
        this.violations = List.copyOf(violations);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public ApiErrorCode getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<ValidationViolation> getViolations() {
        return violations;
    }
}
