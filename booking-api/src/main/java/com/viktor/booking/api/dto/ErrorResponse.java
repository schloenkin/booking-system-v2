package com.viktor.booking.api.dto;

import java.time.Instant;
import java.util.List;

public class ErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final ApiErrorCode code;
    private final String message;
    private final String path;
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
