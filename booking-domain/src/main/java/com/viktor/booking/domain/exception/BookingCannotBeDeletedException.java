package com.viktor.booking.domain.exception;

import com.viktor.booking.domain.enums.BookingStatus;

public class BookingCannotBeDeletedException
        extends RuntimeException {

    public BookingCannotBeDeletedException(
            BookingStatus currentStatus
    ) {
        super(
                "Booking cannot be deleted from status: "
                        + currentStatus
        );
    }
}
