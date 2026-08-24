//package com.viktor.booking.domain.exception;
//
//public class BookingCannotBeRescheduledException extends RuntimeException {
//    public BookingCannotBeRescheduledException(String message) {
//        super(message);
//    }
//}
package com.viktor.booking.domain.exception;

import com.viktor.booking.domain.enums.BookingStatus;

public class BookingCannotBeRescheduledException
        extends RuntimeException {

    public BookingCannotBeRescheduledException(
            BookingStatus currentStatus
    ) {
        super(
                "Booking cannot be rescheduled from status: "
                        + currentStatus
        );
    }
}