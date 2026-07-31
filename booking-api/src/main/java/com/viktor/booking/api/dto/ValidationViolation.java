package com.viktor.booking.api.dto;

public record ValidationViolation(
        String field,
        String message
) {
}
