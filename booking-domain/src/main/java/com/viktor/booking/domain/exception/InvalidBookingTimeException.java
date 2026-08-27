package com.viktor.booking.domain.exception;

public class InvalidBookingTimeException extends RuntimeException {

    public InvalidBookingTimeException(String message) {
        super(message);
    }
}