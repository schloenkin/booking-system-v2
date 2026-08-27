package com.viktor.booking.domain.exception;

public class BookingInPastException extends IllegalArgumentException {

    public BookingInPastException() {
        super("Booking start time must be in the future");
    }
}
