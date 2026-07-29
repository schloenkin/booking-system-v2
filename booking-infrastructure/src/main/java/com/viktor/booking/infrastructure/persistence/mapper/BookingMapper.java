package com.viktor.booking.infrastructure.persistence.mapper;

import com.viktor.booking.domain.model.Booking;
import com.viktor.booking.infrastructure.persistence.entity.BookableServiceEntity;
import com.viktor.booking.infrastructure.persistence.entity.BookingEntity;
import com.viktor.booking.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingEntity toNewEntity(
            Booking booking,
            UserEntity userEntity,
            BookableServiceEntity serviceEntity
    ) {
        if (booking == null) {
            return null;
        }

        if (userEntity == null) {
            throw new IllegalArgumentException("User entity must not be null");
        }

        if (serviceEntity == null) {
            throw new IllegalArgumentException("Service entity must not be null");
        }

        return new BookingEntity(
                userEntity,
                serviceEntity,
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus()
        );
    }

    public Booking toDomain(BookingEntity entity) {
        if (entity == null) {
            return null;
        }

        return Booking.restore(
                entity.getId(),
                entity.getUser().getId(),
                entity.getService().getId(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus()
        );
    }
}
