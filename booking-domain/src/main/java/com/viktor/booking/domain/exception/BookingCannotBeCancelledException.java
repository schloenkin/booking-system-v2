package com.viktor.booking.domain.exception;

import com.viktor.booking.domain.enums.BookingStatus;

public class BookingCannotBeCancelledException
        extends RuntimeException {

    public BookingCannotBeCancelledException(
            BookingStatus currentStatus
    ) {
        super(
                "Booking cannot be cancelled from status: "
                        + currentStatus
        );
    }
}
