package com.viktor.booking.domain.exception;

public class InvalidBookingTimeException extends IllegalArgumentException {

    public InvalidBookingTimeException(String message) {
        super(message);
    }
}