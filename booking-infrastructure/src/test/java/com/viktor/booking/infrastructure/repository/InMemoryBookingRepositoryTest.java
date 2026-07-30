package com.viktor.booking.infrastructure.repository;

import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.model.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

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
                LocalDateTime.of(
                        2026,
                        8,
                        1,
                        10,
                        0
                );

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
        Booking booking = Booking.create(
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
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.update(booking)
        );
    }
}
