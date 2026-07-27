package com.viktor.booking.application.security;

import com.viktor.booking.application.query.BookingSearchCriteria;
import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.Booking;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BookingAccessPolicyTest {

    private final BookingAccessPolicy accessPolicy =
            new BookingAccessPolicy();

    @Test
    void shouldAllowUserToAccessOwnBooking() {
        AuthenticatedUserContext context =
                new AuthenticatedUserContext(
                        7L,
                        UserRole.USER
                );

        Booking booking = createBookingForUser(7L);

        boolean result =
                accessPolicy.canAccess(
                        context,
                        booking
                );

        assertThat(result)
                .isTrue();
    }

    @Test
    void shouldDenyUserAccessToAnotherUsersBooking() {
        AuthenticatedUserContext context =
                new AuthenticatedUserContext(
                        7L,
                        UserRole.USER
                );

        Booking booking = createBookingForUser(9L);

        boolean result =
                accessPolicy.canAccess(
                        context,
                        booking
                );

        assertThat(result)
                .isFalse();
    }

    @Test
    void shouldAllowAdminToAccessAnyBooking() {
        AuthenticatedUserContext context =
                new AuthenticatedUserContext(
                        100L,
                        UserRole.ADMIN
                );

        Booking booking = createBookingForUser(9L);

        boolean result =
                accessPolicy.canAccess(
                        context,
                        booking
                );

        assertThat(result)
                .isTrue();
    }

    @Test
    void shouldRestrictUserSearchToOwnUserId() {
        AuthenticatedUserContext context =
                new AuthenticatedUserContext(
                        7L,
                        UserRole.USER
                );

        LocalDateTime from =
                LocalDateTime.of(
                        2030,
                        1,
                        1,
                        10,
                        0
                );

        LocalDateTime to =
                LocalDateTime.of(
                        2030,
                        1,
                        31,
                        18,
                        0
                );

        BookingSearchCriteria requestedCriteria =
                new BookingSearchCriteria(
                        BookingStatus.PENDING,
                        999L,
                        15L,
                        from,
                        to
                );

        BookingSearchCriteria effectiveCriteria =
                accessPolicy.restrictSearch(
                        context,
                        requestedCriteria
                );

        assertThat(effectiveCriteria.status())
                .isEqualTo(BookingStatus.PENDING);

        assertThat(effectiveCriteria.userId())
                .isEqualTo(7L);

        assertThat(effectiveCriteria.serviceId())
                .isEqualTo(15L);

        assertThat(effectiveCriteria.from())
                .isEqualTo(from);

        assertThat(effectiveCriteria.to())
                .isEqualTo(to);
    }

    @Test
    void shouldNotRestrictAdminSearch() {
        AuthenticatedUserContext context =
                new AuthenticatedUserContext(
                        100L,
                        UserRole.ADMIN
                );

        BookingSearchCriteria requestedCriteria =
                new BookingSearchCriteria(
                        BookingStatus.CONFIRMED,
                        9L,
                        15L,
                        null,
                        null
                );

        BookingSearchCriteria effectiveCriteria =
                accessPolicy.restrictSearch(
                        context,
                        requestedCriteria
                );

        assertThat(effectiveCriteria)
                .isSameAs(requestedCriteria);
    }

    private Booking createBookingForUser(
            Long userId
    ) {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        return new Booking(
                50L,
                userId,
                20L,
                startTime,
                startTime.plusHours(1),
                BookingStatus.PENDING
        );
    }
}