package com.viktor.booking.application.exception;

public class BookingOperationForbiddenException
        extends RuntimeException {

    public BookingOperationForbiddenException(
            String operation
    ) {
        super(
                "Current user is not allowed to "
                        + operation
                        + " booking"
        );
    }
}
