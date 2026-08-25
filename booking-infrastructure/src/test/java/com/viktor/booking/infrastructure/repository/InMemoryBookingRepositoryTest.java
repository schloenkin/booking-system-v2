package com.viktor.booking.infrastructure.repository;

import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.model.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryBookingRepositoryTest {

    private InMemoryBookingRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookingRepository();
    }

    @Test
    void shouldUpdateStatusAndPreserveExistingBookingData() {
        LocalDateTime originalStartTime =
                LocalDateTime.now().plusDays(1);

        LocalDateTime originalEndTime =
                originalStartTime.plusHours(1);

        Booking savedBooking = repository.save(
                Booking.create(
                        1L,
                        10L,
                        originalStartTime,
                        originalEndTime
                )
        );

        Booking incomingBooking = Booking.restore(
                savedBooking.getId(),
                999L,
                888L,
                LocalDateTime.of(
                        2026,
                        9,
                        1,
                        15,
                        0
                ),
                LocalDateTime.of(
                        2026,
                        9,
                        1,
                        16,
                        0
                ),
                BookingStatus.CANCELLED
        );

        Booking updatedBooking = repository
                .update(incomingBooking)
                .orElseThrow();

        assertEquals(
                BookingStatus.CANCELLED,
                updatedBooking.getStatus()
        );

        assertEquals(
                savedBooking.getUserId(),
                updatedBooking.getUserId()
        );

        assertEquals(
                savedBooking.getServiceId(),
                updatedBooking.getServiceId()
        );

        assertEquals(
                savedBooking.getStartTime(),
                updatedBooking.getStartTime()
        );

        assertEquals(
                savedBooking.getEndTime(),
                updatedBooking.getEndTime()
        );
    }

    @Test
    void shouldReturnEmptyWhenBookingDoesNotExist() {
        Booking booking = Booking.restore(
                999L,
                1L,
                10L,
                LocalDateTime.of(
                        2026,
                        8,
                        1,
                        10,
                        0
                ),
                LocalDateTime.of(
                        2026,
                        8,
                        1,
                        11,
                        0
                ),
                BookingStatus.CANCELLED
        );

        Optional<Booking> result =
                repository.update(booking);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectUpdateWithoutBookingId() {
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);
        Booking booking = Booking.create(
                1L,
                10L,
                startTime,
                startTime.plusHours(1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.update(booking)
        );
    }
    @Test
    void shouldNotFindConflictWhenOnlyConflictingBookingIsExcluded() {
        // given
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        LocalDateTime endTime =
                startTime.plusHours(1);

        Booking savedBooking = repository.save(
                Booking.create(
                        1L,
                        10L,
                        startTime,
                        endTime
                )
        );

        // when
        boolean result =
                repository.existsConflictingBookingExcludingId(
                        savedBooking.getId(),
                        savedBooking.getServiceId(),
                        startTime,
                        endTime
                );

        // then
        assertFalse(result);
    }

    @Test
    void shouldFindConflictWhenAnotherBookingOverlaps() {
        // given
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        LocalDateTime endTime =
                startTime.plusHours(1);

        Booking bookingToReschedule = repository.save(
                Booking.create(
                        1L,
                        10L,
                        startTime,
                        endTime
                )
        );

        repository.save(
                Booking.create(
                        2L,
                        10L,
                        startTime.plusMinutes(30),
                        endTime.plusMinutes(30)
                )
        );

        // when
        boolean result =
                repository.existsConflictingBookingExcludingId(
                        bookingToReschedule.getId(),
                        bookingToReschedule.getServiceId(),
                        startTime,
                        endTime
                );

        // then
        assertTrue(result);
    }
    @Test
    void shouldNotFindConflictWhenAnotherBookingDoesNotOverlap() {
        // given
        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        LocalDateTime endTime =
                startTime.plusHours(1);

        Booking bookingToReschedule = repository.save(
                Booking.create(
                        1L,
                        10L,
                        startTime,
                        endTime
                )
        );

        repository.save(
                Booking.create(
                        2L,
                        10L,
                        startTime.plusHours(2),
                        endTime.plusHours(2)
                )
        );

        // when
        boolean result =
                repository.existsConflictingBookingExcludingId(
                        bookingToReschedule.getId(),
                        bookingToReschedule.getServiceId(),
                        startTime,
                        endTime
                );

        // then
        assertFalse(result);
    }
}
