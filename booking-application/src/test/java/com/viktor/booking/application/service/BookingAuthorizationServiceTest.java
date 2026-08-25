package com.viktor.booking.application.service;

import com.viktor.booking.application.exception.BookingOperationForbiddenException;
import com.viktor.booking.application.query.BookingSearchCriteria;
import com.viktor.booking.application.query.PageRequestData;
import com.viktor.booking.application.query.PageResult;
import com.viktor.booking.application.security.AuthenticatedUserContext;
import com.viktor.booking.application.security.BookingAccessPolicy;
import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingAuthorizationServiceTest {

    @Mock
    private BookingService bookingService;

    @Mock
    private PageResult<Booking> pageResult;

    private BookingAuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService =
                new BookingAuthorizationService(
                        bookingService,
                        new BookingAccessPolicy()
                );
    }

    @Test
    void shouldRestrictUserSearchToOwnUserId() {
        AuthenticatedUserContext context =
                userContext(7L);

        BookingSearchCriteria requestedCriteria =
                new BookingSearchCriteria(
                        BookingStatus.PENDING,
                        999L,
                        15L,
                        null,
                        null
                );

        BookingSearchCriteria effectiveCriteria =
                new BookingSearchCriteria(
                        BookingStatus.PENDING,
                        7L,
                        15L,
                        null,
                        null
                );

        PageRequestData pageRequest =
                new PageRequestData(
                        0,
                        20,
                        "startTime",
                        "asc"
                );

        when(bookingService.searchBookings(
                effectiveCriteria,
                pageRequest
        )).thenReturn(pageResult);

        PageResult<Booking> result =
                authorizationService.searchBookings(
                        context,
                        requestedCriteria,
                        pageRequest
                );

        assertThat(result)
                .isSameAs(pageResult);

        verify(bookingService).searchBookings(
                effectiveCriteria,
                pageRequest
        );
    }

    @Test
    void shouldPreserveAdminSearchCriteria() {
        AuthenticatedUserContext context =
                adminContext(100L);

        BookingSearchCriteria requestedCriteria =
                new BookingSearchCriteria(
                        BookingStatus.CONFIRMED,
                        9L,
                        15L,
                        null,
                        null
                );

        PageRequestData pageRequest =
                new PageRequestData(
                        0,
                        20,
                        "startTime",
                        "desc"
                );

        when(bookingService.searchBookings(
                requestedCriteria,
                pageRequest
        )).thenReturn(pageResult);

        PageResult<Booking> result =
                authorizationService.searchBookings(
                        context,
                        requestedCriteria,
                        pageRequest
                );

        assertThat(result)
                .isSameAs(pageResult);

        verify(bookingService).searchBookings(
                requestedCriteria,
                pageRequest
        );
    }

    @Test
    void shouldReturnUsersOwnBooking() {
        AuthenticatedUserContext context =
                userContext(7L);

        Booking booking =
                bookingForUser(
                        50L,
                        7L,
                        BookingStatus.PENDING
                );

        when(bookingService.getBookingById(50L))
                .thenReturn(Optional.of(booking));

        Optional<Booking> result =
                authorizationService.getBookingById(
                        context,
                        50L
                );

        assertThat(result)
                .containsSame(booking);
    }

    @Test
    void shouldHideAnotherUsersBooking() {
        AuthenticatedUserContext context =
                userContext(7L);

        Booking anotherUsersBooking =
                bookingForUser(
                        50L,
                        9L,
                        BookingStatus.PENDING
                );

        when(bookingService.getBookingById(50L))
                .thenReturn(
                        Optional.of(
                                anotherUsersBooking
                        )
                );

        Optional<Booking> result =
                authorizationService.getBookingById(
                        context,
                        50L
                );

        assertThat(result)
                .isEmpty();
    }

    @Test
    void shouldAllowAdminToReadAnyBooking() {
        AuthenticatedUserContext context =
                adminContext(100L);

        Booking booking =
                bookingForUser(
                        50L,
                        9L,
                        BookingStatus.PENDING
                );

        when(bookingService.getBookingById(50L))
                .thenReturn(Optional.of(booking));

        Optional<Booking> result =
                authorizationService.getBookingById(
                        context,
                        50L
                );

        assertThat(result)
                .containsSame(booking);
    }

    @Test
    void shouldCreateBookingForAuthenticatedUser() {
        AuthenticatedUserContext context =
                userContext(7L);

        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        1,
                        15,
                        10,
                        0
                );

        LocalDateTime endTime =
                startTime.plusHours(1);

        Booking createdBooking =
                bookingForUser(
                        60L,
                        7L,
                        BookingStatus.PENDING
                );

        when(bookingService.createBooking(
                7L,
                15L,
                startTime,
                endTime
        )).thenReturn(createdBooking);

        Booking result =
                authorizationService.createBooking(
                        context,
                        15L,
                        startTime,
                        endTime
                );

        assertThat(result)
                .isSameAs(createdBooking);

        verify(bookingService).createBooking(
                7L,
                15L,
                startTime,
                endTime
        );
    }

    @Test
    void shouldNotDeleteAnotherUsersBooking() {
        AuthenticatedUserContext context =
                userContext(7L);

        Booking anotherUsersBooking =
                bookingForUser(
                        70L,
                        9L,
                        BookingStatus.PENDING
                );

        when(bookingService.getBookingById(70L))
                .thenReturn(
                        Optional.of(
                                anotherUsersBooking
                        )
                );

        boolean result =
                authorizationService.deleteBookingById(
                        context,
                        70L
                );

        assertThat(result)
                .isFalse();

        verify(bookingService, never())
                .deleteBookingById(70L);
    }

    @Test
    void shouldRejectDeletingUsersOwnBooking() {
        AuthenticatedUserContext context =
                userContext(7L);

        Booking booking =
                bookingForUser(
                        70L,
                        7L,
                        BookingStatus.CANCELLED
                );

        when(bookingService.getBookingById(70L))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() ->
                authorizationService.deleteBookingById(
                        context,
                        70L
                )
        )
                .isInstanceOf(
                        BookingOperationForbiddenException.class
                )
                .hasMessage(
                        "Current user is not allowed to delete booking"
                );

        verify(bookingService)
                .getBookingById(70L);

        verify(bookingService, never())
                .deleteBookingById(70L);
    }

    @Test
    void shouldAllowAdminToDeleteBooking() {
        AuthenticatedUserContext context =
                adminContext(100L);

        Booking cancelledBooking =
                bookingForUser(
                        70L,
                        9L,
                        BookingStatus.CANCELLED
                );

        when(bookingService.getBookingById(70L))
                .thenReturn(
                        Optional.of(
                                cancelledBooking
                        )
                );

        when(bookingService.deleteBookingById(70L))
                .thenReturn(true);

        boolean result =
                authorizationService.deleteBookingById(
                        context,
                        70L
                );

        assertThat(result)
                .isTrue();

        verify(bookingService)
                .getBookingById(70L);

        verify(bookingService)
                .deleteBookingById(70L);
    }

    @Test
    void shouldNotCancelAnotherUsersBooking() {
        AuthenticatedUserContext context =
                userContext(7L);

        Booking anotherUsersBooking =
                bookingForUser(
                        80L,
                        9L,
                        BookingStatus.PENDING
                );

        when(bookingService.getBookingById(80L))
                .thenReturn(
                        Optional.of(
                                anotherUsersBooking
                        )
                );

        Optional<Booking> result =
                authorizationService.cancelBookingById(
                        context,
                        80L
                );

        assertThat(result)
                .isEmpty();

        verify(bookingService, never())
                .cancelBookingById(80L);
    }

    @Test
    void shouldCancelUsersOwnBooking() {
        AuthenticatedUserContext context =
                userContext(7L);

        Booking pendingBooking =
                bookingForUser(
                        80L,
                        7L,
                        BookingStatus.PENDING
                );

        Booking cancelledBooking =
                bookingForUser(
                        80L,
                        7L,
                        BookingStatus.CANCELLED
                );

        when(bookingService.getBookingById(80L))
                .thenReturn(
                        Optional.of(
                                pendingBooking
                        )
                );

        when(bookingService.cancelBookingById(80L))
                .thenReturn(
                        Optional.of(
                                cancelledBooking
                        )
                );

        Optional<Booking> result =
                authorizationService.cancelBookingById(
                        context,
                        80L
                );

        assertThat(result)
                .containsSame(cancelledBooking);

        verify(bookingService)
                .cancelBookingById(80L);
    }

    @Test
    void shouldNotConfirmAnotherUsersBooking() {
        AuthenticatedUserContext context =
                userContext(7L);

        Booking anotherUsersBooking =
                bookingForUser(
                        90L,
                        9L,
                        BookingStatus.PENDING
                );

        when(bookingService.getBookingById(90L))
                .thenReturn(
                        Optional.of(
                                anotherUsersBooking
                        )
                );

        Optional<Booking> result =
                authorizationService.confirmBookingById(
                        context,
                        90L
                );

        assertThat(result)
                .isEmpty();

        verify(bookingService, never())
                .confirmBookingById(90L);
    }

    @Test
    void shouldRejectUserConfirmationOfOwnBooking() {
        AuthenticatedUserContext context =
                userContext(7L);

        Booking ownPendingBooking =
                bookingForUser(
                        90L,
                        7L,
                        BookingStatus.PENDING
                );

        when(bookingService.getBookingById(90L))
                .thenReturn(
                        Optional.of(
                                ownPendingBooking
                        )
                );

        assertThatThrownBy(() ->
                authorizationService.confirmBookingById(
                        context,
                        90L
                )
        )
                .isInstanceOf(
                        BookingOperationForbiddenException.class
                )
                .hasMessage(
                        "Current user is not allowed to confirm booking"
                );

        verify(bookingService)
                .getBookingById(90L);

        verify(bookingService, never())
                .confirmBookingById(90L);
    }

    @Test
    void shouldAllowAdminToConfirmAnyBooking() {
        AuthenticatedUserContext context =
                adminContext(100L);

        Booking pendingBooking =
                bookingForUser(
                        90L,
                        9L,
                        BookingStatus.PENDING
                );

        Booking confirmedBooking =
                bookingForUser(
                        90L,
                        9L,
                        BookingStatus.CONFIRMED
                );

        when(bookingService.getBookingById(90L))
                .thenReturn(
                        Optional.of(
                                pendingBooking
                        )
                );

        when(bookingService.confirmBookingById(90L))
                .thenReturn(
                        Optional.of(
                                confirmedBooking
                        )
                );

        Optional<Booking> result =
                authorizationService.confirmBookingById(
                        context,
                        90L
                );

        assertThat(result)
                .containsSame(confirmedBooking);

        verify(bookingService)
                .confirmBookingById(90L);
    }

    @Test
    void shouldRescheduleUsersOwnBooking() {
        AuthenticatedUserContext context =
                userContext(7L);

        Booking pendingBooking =
                bookingForUser(
                        80L,
                        7L,
                        BookingStatus.PENDING
                );

        LocalDateTime newStartTime =
                LocalDateTime.now().plusDays(2);

        LocalDateTime newEndTime =
                newStartTime.plusHours(1);

        when(bookingService.getBookingById(80L))
                .thenReturn(
                        Optional.of(
                                pendingBooking
                        )
                );

        when(bookingService.rescheduleBookingById(
                80L,
                newStartTime,
                newEndTime
        ))
                .thenReturn(
                        Optional.of(
                                pendingBooking
                        )
                );

        Optional<Booking> result =
                authorizationService.rescheduleBookingById(
                        context,
                        80L,
                        newStartTime,
                        newEndTime
                );

        assertThat(result)
                .containsSame(pendingBooking);

        verify(bookingService)
                .rescheduleBookingById(
                        80L,
                        newStartTime,
                        newEndTime
                );
    }

    @Test
    void shouldNotRescheduleAnotherUsersBooking() {
        AuthenticatedUserContext context =
                userContext(7L);

        Booking anotherUsersBooking  =
                bookingForUser(
                        80L,
                        9L,
                        BookingStatus.PENDING
                );

        LocalDateTime newStartTime =
                LocalDateTime.now().plusDays(2);

        LocalDateTime newEndTime =
                newStartTime.plusHours(1);

        when(bookingService.getBookingById(80L))
                .thenReturn(
                        Optional.of(
                                anotherUsersBooking
                        )
                );

        Optional<Booking> result =
                authorizationService.rescheduleBookingById(
                        context,
                        80L,
                        newStartTime,
                        newEndTime
                );

        assertThat(result)
                .isEmpty();

        verify(bookingService, never())
                .rescheduleBookingById(
                        80L,
                        newStartTime,
                        newEndTime
                );
    }

    private AuthenticatedUserContext userContext(
            Long userId
    ) {
        return new AuthenticatedUserContext(
                userId,
                UserRole.USER
        );
    }

    private AuthenticatedUserContext adminContext(
            Long userId
    ) {
        return new AuthenticatedUserContext(
                userId,
                UserRole.ADMIN
        );
    }

    private Booking bookingForUser(
            Long bookingId,
            Long userId,
            BookingStatus status
    ) {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        return Booking.restore(
                bookingId,
                userId,
                15L,
                startTime,
                startTime.plusHours(1),
                status
        );
    }

}
