package com.viktor.booking.domain.model;

import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.exception.*;

import java.time.LocalDateTime;

public class Booking {

    private Long id;
    private Long userId;
    private Long serviceId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;

    public static Booking create(
            Long userId,
            Long serviceId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (startTime != null && !startTime.isAfter(LocalDateTime.now())) {
            throw new BookingInPastException();
        }
        return new Booking(
                null,
                userId,
                serviceId,
                startTime,
                endTime,
                BookingStatus.PENDING
        );
    }

    public static Booking restore(
            Long id,
            Long userId,
            Long serviceId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BookingStatus status
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "Booking id must not be null"
            );
        }

        return new Booking(
                id,
                userId,
                serviceId,
                startTime,
                endTime,
                status
        );
    }

    private Booking(Long id, Long userId, Long serviceId, LocalDateTime startTime, LocalDateTime endTime, BookingStatus status) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "User id must not be null"
            );
        }

        if (serviceId == null) {
            throw new IllegalArgumentException(
                    "Service id must not be null"
            );
        }

        if (startTime == null) {
            throw new IllegalArgumentException("Start time must not be null");
        }

        if (endTime == null) {
            throw new IllegalArgumentException("End time must not be null");
        }

        if (!endTime.isAfter(startTime)) {
            throw new InvalidBookingTimeException("End time must be after start time");
        }
        if (status==null) {
            throw new IllegalArgumentException("Booking status must not be null");
        }

        this.id = id;
        this.userId = userId;
        this.serviceId = serviceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void confirm() {
        if (status != BookingStatus.PENDING) {
            throw new BookingCannotBeConfirmedException(status);
        }

        this.status = BookingStatus.CONFIRMED;
    }

    public void cancel() {
        if (status != BookingStatus.PENDING
                && status != BookingStatus.CONFIRMED) {
            throw new BookingCannotBeCancelledException(status);
        }

        this.status = BookingStatus.CANCELLED;
    }

    public void ensureCanBeDeleted() {
        if (status != BookingStatus.CANCELLED) {
            throw new BookingCannotBeDeletedException(
                    status
            );
        }
    }

    public void reschedule(LocalDateTime newStartTime, LocalDateTime newEndTime){
        if (status == BookingStatus.CANCELLED) {
            throw new BookingCannotBeRescheduledException(
                    status
            );
        }

        if (newStartTime == null) {
            throw new IllegalArgumentException(
                    "Start time must not be null"
            );
        }

        if (newEndTime == null) {
            throw new IllegalArgumentException(
                    "End time must not be null"
            );
        }

        if (!newStartTime.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Start time must be in the future"
            );
        }

        if (!newEndTime.isAfter(newStartTime)) {
            throw new IllegalArgumentException(
                    "End time must be after start time"
            );
        }

        this.startTime = newStartTime;
        this.endTime = newEndTime;
    }

}