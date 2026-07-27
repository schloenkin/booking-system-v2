package com.viktor.booking.application.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(Throwable cause) {
        super(
                "Invalid or expired token",
                cause
        );
    }
}