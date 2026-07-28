package com.viktor.booking.domain.model;

import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.exception.BookingCannotBeCancelledException;
import com.viktor.booking.domain.exception.BookingCannotBeConfirmedException;
import com.viktor.booking.domain.exception.BookingCannotBeDeletedException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class BookingTest {

    @Test
    void shouldConfirmPendingBooking() {
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        Booking booking = new Booking(
                1L,
                1L,
                2L,
                startTime,
                startTime.plusMinutes(60),
                BookingStatus.PENDING
        );

        booking.confirm();

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void shouldRejectConfirmationWhenBookingIsAlreadyConfirmed() {
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        Booking booking = new Booking(
                1L,
                1L,
                2L,
                startTime,
                startTime.plusMinutes(60),
                BookingStatus.CONFIRMED
        );

        assertThatThrownBy(booking::confirm)
                .isInstanceOf(
                        BookingCannotBeConfirmedException.class
                )
                .hasMessage(
                        "Booking cannot be confirmed from status: CONFIRMED"
                );

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void shouldRejectConfirmationWhenBookingIsCancelled() {
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        Booking booking = new Booking(
                1L,
                1L,
                2L,
                startTime,
                startTime.plusMinutes(60),
                BookingStatus.CANCELLED
        );

        assertThatThrownBy(booking::confirm)
                .isInstanceOf(
                        BookingCannotBeConfirmedException.class
                )
                .hasMessage(
                        "Booking cannot be confirmed from status: CANCELLED"
                );

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }
    @Test
    void shouldCancelPendingBooking() {
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        Booking booking = new Booking(
                1L,
                1L,
                2L,
                startTime,
                startTime.plusMinutes(60),
                BookingStatus.PENDING
        );

        booking.cancel();

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void shouldCancelConfirmedBooking() {
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        Booking booking = new Booking(
                1L,
                1L,
                2L,
                startTime,
                startTime.plusMinutes(60),
                BookingStatus.CONFIRMED
        );

        booking.cancel();

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void shouldRejectCancellationWhenBookingIsAlreadyCancelled() {
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        Booking booking = new Booking(
                1L,
                1L,
                2L,
                startTime,
                startTime.plusMinutes(60),
                BookingStatus.CANCELLED
        );

        assertThatThrownBy(booking::cancel)
                .isInstanceOf(
                        BookingCannotBeCancelledException.class
                )
                .hasMessage(
                        "Booking cannot be cancelled from status: CANCELLED"
                );

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void shouldAllowCancelledBookingToBeDeleted() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        Booking booking = new Booking(
                50L,
                7L,
                20L,
                startTime,
                startTime.plusHours(1),
                BookingStatus.CANCELLED
        );

        booking.ensureCanBeDeleted();

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void shouldRejectDeletingPendingBooking() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        Booking booking = new Booking(
                50L,
                7L,
                20L,
                startTime,
                startTime.plusHours(1),
                BookingStatus.PENDING
        );

        assertThatThrownBy(
                booking::ensureCanBeDeleted
        )
                .isInstanceOf(
                        BookingCannotBeDeletedException.class
                )
                .hasMessage(
                        "Booking cannot be deleted from status: PENDING"
                );

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void shouldRejectDeletingConfirmedBooking() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        Booking booking = new Booking(
                50L,
                7L,
                20L,
                startTime,
                startTime.plusHours(1),
                BookingStatus.CONFIRMED
        );

        assertThatThrownBy(
                booking::ensureCanBeDeleted
        )
                .isInstanceOf(
                        BookingCannotBeDeletedException.class
                )
                .hasMessage(
                        "Booking cannot be deleted from status: CONFIRMED"
                );

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

}
