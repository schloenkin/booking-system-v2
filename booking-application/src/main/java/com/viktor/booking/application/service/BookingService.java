package com.viktor.booking.application.service;

import com.viktor.booking.domain.exception.BookingCannotBeConfirmedException;
import com.viktor.booking.domain.exception.InvalidBookingTimeException;
import com.viktor.booking.application.repository.BookingRepository;
import com.viktor.booking.application.exception.BookableServiceNotFoundException;
import com.viktor.booking.application.repository.BookableServiceRepository;
import com.viktor.booking.application.repository.UserRepository;
import com.viktor.booking.application.exception.UserNotFoundException;
import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.model.Booking;
import org.springframework.stereotype.Service;
import com.viktor.booking.application.exception.InactiveBookableServiceException;
import com.viktor.booking.domain.model.BookableService;
import com.viktor.booking.application.exception.InvalidBookingDurationException;
import com.viktor.booking.domain.exception.BookingInPastException;
import com.viktor.booking.application.exception.BookingTimeConflictException;
import org.springframework.transaction.annotation.Transactional;
import com.viktor.booking.application.exception.InvalidBookingSearchException;
import com.viktor.booking.application.query.BookingSearchCriteria;
import com.viktor.booking.application.query.PageRequestData;
import com.viktor.booking.application.query.PageResult;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.time.Duration;
import java.util.Set;

@Service
public class BookingService {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of(
                    "id",
                    "startTime",
                    "endTime",
                    "status"
            );

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BookableServiceRepository serviceRepository;

    public BookingService(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            BookableServiceRepository serviceRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public PageResult<Booking> searchBookings(
            BookingSearchCriteria criteria,
            PageRequestData pageRequest
    ) {
        if (pageRequest.page() < 0) {
            throw new InvalidBookingSearchException(
                    "Page number must not be negative"
            );
        }

        if (pageRequest.size() < 1
                || pageRequest.size() > 100) {
            throw new InvalidBookingSearchException(
                    "Page size must be between 1 and 100"
            );
        }

        if (criteria.from() != null
                && criteria.to() != null
                && criteria.from().isAfter(criteria.to())) {
            throw new InvalidBookingSearchException(
                    "Date from must not be after date to"
            );
        }

        if (pageRequest.sortBy() == null
                || !ALLOWED_SORT_FIELDS.contains(
                pageRequest.sortBy()
        )) {
            throw new InvalidBookingSearchException(
                    "Unsupported booking sort field: "
                            + pageRequest.sortBy()
            );
        }

        if (!"asc".equalsIgnoreCase(
                pageRequest.direction()
        )
                && !"desc".equalsIgnoreCase(
                pageRequest.direction()
        )) {
            throw new InvalidBookingSearchException(
                    "Sort direction must be asc or desc"
            );
        }

        return bookingRepository.search(
                criteria,
                pageRequest
        );
    }

    @Transactional
    public Booking createBooking(Long userId, Long serviceId, LocalDateTime startTime, LocalDateTime endTime) {
        Booking booking = Booking.create(
                userId,
                serviceId,
                startTime,
                endTime
        );

        if (userRepository.findById(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        BookableService service = serviceRepository
                .findByIdForUpdate(serviceId)
                .orElseThrow(() ->
                        new BookableServiceNotFoundException(serviceId)
                );

        if (!service.isActive()) {
            throw new InactiveBookableServiceException(serviceId);
        }
        Duration bookingDuration =
                Duration.between(startTime, endTime);

        Duration requiredDuration =
                Duration.ofMinutes(service.getDurationMinutes());

        if (!bookingDuration.equals(requiredDuration)) {
            throw new InvalidBookingDurationException(
                    service.getDurationMinutes()
            );
        }

        if (bookingRepository.existsConflictingBooking(
                serviceId,
                startTime,
                endTime
        )) {
            throw new BookingTimeConflictException(serviceId);
        }

        return bookingRepository.save(booking);
    }

    @Transactional
    public boolean deleteBookingById(Long id) {
        return bookingRepository.findById(id)
                        .map(booking -> {
                                    booking.ensureCanBeDeleted();
                                    bookingRepository.deleteById(id);
                            return true;
                        })
                .orElse(
                        false
                );
    }

    @Transactional
    public Optional<Booking> cancelBookingById(Long id) {
        return bookingRepository.findById(id)
                .flatMap(existingBooking -> {
                    existingBooking.cancel();
                    return bookingRepository.update(existingBooking);
                });
    }

    @Transactional
    public Optional<Booking> confirmBookingById(Long id) {
        return bookingRepository.findById(id)
                .flatMap(existingBooking -> {
                    existingBooking.confirm();
                    return bookingRepository.update(existingBooking);
                });
    }

    @Transactional
    public Optional<Booking> rescheduleBookingById(
            Long bookingId,
            LocalDateTime newStartTime,
            LocalDateTime newEndTime
    ) {
        return bookingRepository.findById(bookingId)
                .flatMap(existingBooking -> {

        if (bookingRepository.existsConflictingBookingExcludingId(
                bookingId,
                existingBooking.getServiceId(),
                newStartTime,
                newEndTime
        )) {
            throw new BookingTimeConflictException(
                    existingBooking.getServiceId()
            );
        }

        existingBooking.reschedule(
                newStartTime,
                newEndTime
        );

        return bookingRepository.update(
                existingBooking);
                });
    }
}