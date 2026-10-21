package com.viktor.booking.domain.exception;

public class InvalidBookingScheduleException
        extends RuntimeException {

    public InvalidBookingScheduleException() {
        super("Booking is outside the allowed schedule");
    }
}