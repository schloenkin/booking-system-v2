package com.viktor.booking.infrastructure.repository;

import com.viktor.booking.application.repository.BookingRepository;
import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.model.Booking;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;
import com.viktor.booking.application.query.BookingSearchCriteria;
import com.viktor.booking.application.query.PageRequestData;
import com.viktor.booking.application.query.PageResult;

import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!jpa")
public class InMemoryBookingRepository implements BookingRepository {

    private final List<Booking> bookings = new ArrayList<>();
    private Long nextId = 3L;

    public InMemoryBookingRepository() {
        bookings.add(Booking.restore(
                1L,
                1L,
                1L,
                LocalDateTime.of(2026, 6, 25, 10, 0),
                LocalDateTime.of(2026, 6, 25, 10, 30),
                BookingStatus.CONFIRMED
        ));

        bookings.add(Booking.restore(
                2L,
                2L,
                2L,
                LocalDateTime.of(2026, 6, 25, 11, 0),
                LocalDateTime.of(2026, 6, 25, 12, 0),
                BookingStatus.PENDING
        ));
    }

    @Override
    public List<Booking> findAll() {
        return bookings;
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return bookings
                .stream()
                .filter(booking -> booking.getId().equals(id))
                .findFirst();
    }
    @Override
    public List<Booking> findByStatus(BookingStatus status) {
        return bookings
                .stream()
                .filter(booking -> booking.getStatus() == status)
                .toList();
    }

    @Override
    public PageResult<Booking> search(
            BookingSearchCriteria criteria,
            PageRequestData pageRequest
    ) {
        Comparator<Booking> comparator =
                createComparator(pageRequest.sortBy());

        if ("desc".equalsIgnoreCase(pageRequest.direction())) {
            comparator = comparator.reversed();
        }

        List<Booking> filteredBookings = bookings.stream()
                .filter(booking ->
                        criteria.status() == null
                                || booking.getStatus()
                                == criteria.status()
                )
                .filter(booking ->
                        criteria.userId() == null
                                || booking.getUserId()
                                .equals(criteria.userId())
                )
                .filter(booking ->
                        criteria.serviceId() == null
                                || booking.getServiceId()
                                .equals(criteria.serviceId())
                )
                .filter(booking ->
                        criteria.from() == null
                                || !booking.getStartTime()
                                .isBefore(criteria.from())
                )
                .filter(booking ->
                        criteria.to() == null
                                || !booking.getStartTime()
                                .isAfter(criteria.to())
                )
                .sorted(comparator)
                .toList();

        long totalElements = filteredBookings.size();

        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil(
                (double) totalElements / pageRequest.size()
        );

        long offset =
                (long) pageRequest.page() * pageRequest.size();

        int fromIndex = (int) Math.min(
                offset,
                totalElements
        );

        int toIndex = (int) Math.min(
                offset + pageRequest.size(),
                totalElements
        );

        List<Booking> content =
                filteredBookings.subList(fromIndex, toIndex);

        boolean first = pageRequest.page() == 0;

        boolean last = totalPages == 0
                || pageRequest.page() >= totalPages - 1;

        return new PageResult<>(
                content,
                pageRequest.page(),
                pageRequest.size(),
                totalElements,
                totalPages,
                first,
                last
        );
    }

    @Override
    public boolean existsConflictingBooking(
            Long serviceId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return bookings.stream()
                .filter(booking ->
                        booking.getServiceId().equals(serviceId)
                )
                .filter(booking ->
                        booking.getStatus() != BookingStatus.CANCELLED
                )
                .anyMatch(booking ->
                        booking.getStartTime().isBefore(endTime)
                                && booking.getEndTime().isAfter(startTime)
                );
    }

    @Override
    public Booking save(Booking booking) {
        Booking savedBooking = Booking.restore(
                nextId,
                booking.getUserId(),
                booking.getServiceId(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus()
        );

        bookings.add(savedBooking);
        nextId++;

        return savedBooking;
    }

    @Override
    public Optional<Booking> update(
            Booking booking
    ) {
        if (booking.getId() == null) {
            throw new IllegalArgumentException(
                    "Booking id must not be null for update"
            );
        }

        for (int index = 0; index < bookings.size(); index++) {
            Booking existingBooking =
                    bookings.get(index);

            if (existingBooking.getId()
                    .equals(booking.getId())) {

                Booking updatedBooking =
                        Booking.restore(
                                existingBooking.getId(),
                                existingBooking.getUserId(),
                                existingBooking.getServiceId(),
                                existingBooking.getStartTime(),
                                existingBooking.getEndTime(),
                                booking.getStatus()
                        );


                bookings.set(
                        index,
                        updatedBooking
                );

                return Optional.of(
                        updatedBooking
                );
            }
        }

        return Optional.empty();
    }

    @Override
    public void deleteById(Long id) {
        bookings.removeIf(booking -> booking.getId().equals(id));
    }

    private Comparator<Booking> createComparator(
            String sortBy
    ) {
        Comparator<Booking> comparator = switch (sortBy) {
            case "id" ->
                    Comparator.comparing(
                            Booking::getId,
                            Comparator.nullsLast(Long::compareTo)
                    );

            case "endTime" ->
                    Comparator.comparing(
                            Booking::getEndTime
                    );

            case "status" ->
                    Comparator.comparing(
                            Booking::getStatus
                    );

            case "startTime" ->
                    Comparator.comparing(
                            Booking::getStartTime
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported booking sort field: "
                                    + sortBy
                    );
        };

        return comparator.thenComparing(
                Booking::getId,
                Comparator.nullsLast(Long::compareTo)
        );
    }
}