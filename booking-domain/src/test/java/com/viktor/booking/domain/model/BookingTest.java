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

        Booking booking = Booking.restore(
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
    void shouldRejectBookingWhenStartTimeIsInThePast() {
        LocalDateTime startTime =
                LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() ->
                Booking.create(
                        1L,
                        2L,
                        startTime,
                        startTime.plusMinutes(60)
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectConfirmationWhenBookingIsAlreadyConfirmed() {
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        Booking booking = Booking.restore(
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

        Booking booking = Booking.restore(
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

        Booking booking = Booking.restore(
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

        Booking booking = Booking.restore(
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

        Booking booking = Booking.restore(
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

        Booking booking = Booking.restore(
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

        Booking booking = Booking.restore(
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

        Booking booking = Booking.restore(
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
    @Test
    void shouldRejectBookingWhenStartTimeIsNull() {
        LocalDateTime endTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        11,
                        0
                );

        assertThatThrownBy(() -> Booking.create(
                1L,
                2L,
                null,
                endTime
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Start time must not be null"
                );
    }

    @Test
    void shouldRejectBookingWhenEndTimeIsNull() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        assertThatThrownBy(() -> Booking.create(
                1L,
                2L,
                startTime,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "End time must not be null"
                );
    }

    @Test
    void shouldRejectBookingWhenEndTimeEqualsStartTime() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        assertThatThrownBy(() -> Booking.create(
                1L,
                2L,
                startTime,
                startTime
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "End time must be after start time"
                );
    }

    @Test
    void shouldRejectBookingWhenEndTimeIsBeforeStartTime() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        LocalDateTime endTime =
                startTime.minusMinutes(30);

        assertThatThrownBy(() -> Booking.create(
                1L,
                2L,
                startTime,
                endTime
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "End time must be after start time"
                );
    }

    @Test
    void shouldRejectRestorationWhenEndTimeIsBeforeStartTime() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        LocalDateTime endTime =
                startTime.minusMinutes(30);

        assertThatThrownBy(() -> Booking.restore(
                10L,
                1L,
                2L,
                startTime,
                endTime,
                BookingStatus.CONFIRMED
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "End time must be after start time"
                );
    }

    @Test
    void shouldRejectRestorationWhenStatusIsNull() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        assertThatThrownBy(() -> Booking.restore(
                50L,
                7L,
                15L,
                startTime,
                startTime.plusHours(1),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Booking status must not be null"
                );
    }

    @Test
    void shouldCreateNewPendingBookingWithoutId() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        Booking booking = Booking.create(
                7L,
                15L,
                startTime,
                startTime.plusHours(1)
        );

        assertThat(booking.getId())
                .isNull();

        assertThat(booking.getUserId())
                .isEqualTo(7L);

        assertThat(booking.getServiceId())
                .isEqualTo(15L);

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void shouldRestoreExistingBookingWithSavedState() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        Booking booking = Booking.restore(
                50L,
                7L,
                15L,
                startTime,
                startTime.plusHours(1),
                BookingStatus.CONFIRMED
        );

        assertThat(booking.getId())
                .isEqualTo(50L);

        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void shouldRejectRestorationWhenIdIsNull() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        assertThatThrownBy(() -> Booking.restore(
                null,
                7L,
                15L,
                startTime,
                startTime.plusHours(1),
                BookingStatus.PENDING
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Booking id must not be null"
                );
    }

    @Test
    void shouldRejectBookingWhenUserIdIsNull() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        assertThatThrownBy(() -> Booking.create(
                null,
                15L,
                startTime,
                startTime.plusHours(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "User id must not be null"
                );
    }

    @Test
    void shouldRejectBookingWhenServiceIdIsNull() {
        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        2,
                        1,
                        10,
                        0
                );

        assertThatThrownBy(() -> Booking.create(
                7L,
                null,
                startTime,
                startTime.plusHours(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Service id must not be null"
                );
    }

}
