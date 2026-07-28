package com.viktor.booking.application.security;

import com.viktor.booking.application.query.BookingSearchCriteria;
import com.viktor.booking.domain.model.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingAccessPolicy {

    public boolean canAccess(
            AuthenticatedUserContext context,
            Booking booking
    ) {
        return context.isAdmin()
                || context.userId()
                .equals(booking.getUserId());
    }

    public boolean canConfirm(
            AuthenticatedUserContext context
    ) {
        return context.isAdmin();
    }

    public boolean canCancel(
            AuthenticatedUserContext context,
            Booking booking
    ) {
        return canAccess(
                context,
                booking
        );
    }

    public boolean canDelete(
            AuthenticatedUserContext context
    ) {
        return context.isAdmin();
    }

    public BookingSearchCriteria restrictSearch(
            AuthenticatedUserContext context,
            BookingSearchCriteria requestedCriteria
    ) {
        if (context.isAdmin()) {
            return requestedCriteria;
        }

        return new BookingSearchCriteria(
                requestedCriteria.status(),
                context.userId(),
                requestedCriteria.serviceId(),
                requestedCriteria.from(),
                requestedCriteria.to()
        );
    }
}
