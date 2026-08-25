package com.viktor.booking.application.service;

import com.viktor.booking.application.exception.BookingOperationForbiddenException;
import com.viktor.booking.application.query.BookingSearchCriteria;
import com.viktor.booking.application.query.PageRequestData;
import com.viktor.booking.application.query.PageResult;
import com.viktor.booking.application.security.AuthenticatedUserContext;
import com.viktor.booking.application.security.BookingAccessPolicy;
import com.viktor.booking.domain.model.Booking;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BookingAuthorizationService {

    private final BookingService bookingService;
    private final BookingAccessPolicy accessPolicy;

    public BookingAuthorizationService(
            BookingService bookingService,
            BookingAccessPolicy accessPolicy
    ) {
        this.bookingService = bookingService;
        this.accessPolicy = accessPolicy;
    }

    public PageResult<Booking> searchBookings(
            AuthenticatedUserContext context,
            BookingSearchCriteria requestedCriteria,
            PageRequestData pageRequest
    ) {
        BookingSearchCriteria effectiveCriteria =
                accessPolicy.restrictSearch(
                        context,
                        requestedCriteria
                );

        return bookingService.searchBookings(
                effectiveCriteria,
                pageRequest
        );
    }

    public Optional<Booking> getBookingById(
            AuthenticatedUserContext context,
            Long bookingId
    ) {
        return findAccessibleBooking(
                context,
                bookingId
        );
    }

    public Booking createBooking(
            AuthenticatedUserContext context,
            Long serviceId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return bookingService.createBooking(
                context.userId(),
                serviceId,
                startTime,
                endTime
        );
    }

    public boolean deleteBookingById(
            AuthenticatedUserContext context,
            Long bookingId
    ) {
        Optional<Booking> accessibleBooking =
                findAccessibleBooking(
                        context,
                        bookingId
                );

        if (accessibleBooking.isEmpty()) {
            return false;
        }

        if (!accessPolicy.canDelete(context)) {
            throw new BookingOperationForbiddenException(
                    "delete"
            );
        }

        return bookingService.deleteBookingById(
                bookingId
        );
    }

    public Optional<Booking> cancelBookingById(
            AuthenticatedUserContext context,
            Long bookingId
    ) {
        Optional<Booking> accessibleBooking =
                findAccessibleBooking(
                        context,
                        bookingId
                );

        if (accessibleBooking.isEmpty()) {
            return Optional.empty();
        }

        Booking booking = accessibleBooking.get();

        if (!accessPolicy.canCancel(
                context,
                booking
        )) {
            throw new BookingOperationForbiddenException(
                    "cancel"
            );
        }

        return bookingService.cancelBookingById(
                bookingId
        );
    }

    public Optional<Booking> confirmBookingById(
            AuthenticatedUserContext context,
            Long bookingId
    ) {
        Optional<Booking> accessibleBooking =
                findAccessibleBooking(
                        context,
                        bookingId
                );

        if (accessibleBooking.isEmpty()) {
            return Optional.empty();
        }

        if (!accessPolicy.canConfirm(context)) {
            throw new BookingOperationForbiddenException(
                    "confirm"
            );
        }

        return bookingService.confirmBookingById(
                bookingId
        );
    }
    public Optional<Booking> rescheduleBookingById(
            AuthenticatedUserContext context,
            Long bookingId,
            LocalDateTime newStartTime,
            LocalDateTime newEndTime
    ) {
        Optional<Booking> accessibleBooking =
                findAccessibleBooking(
                        context,
                        bookingId
                );

        if (accessibleBooking.isEmpty()) {
            return Optional.empty();
        }

        return bookingService.rescheduleBookingById(
                bookingId,
                newStartTime,
                newEndTime
        );
    }

    private Optional<Booking> findAccessibleBooking(
            AuthenticatedUserContext context,
            Long bookingId
    ) {
        return bookingService
                .getBookingById(bookingId)
                .filter(booking ->
                        accessPolicy.canAccess(
                                context,
                                booking
                        )
                );
    }
}