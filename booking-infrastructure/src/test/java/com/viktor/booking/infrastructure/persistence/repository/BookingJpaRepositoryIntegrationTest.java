package com.viktor.booking.infrastructure.persistence.repository;

import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.infrastructure.persistence.entity.BookableServiceEntity;
import com.viktor.booking.infrastructure.persistence.entity.BookingEntity;
import com.viktor.booking.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(BookingJpaRepositoryIntegrationTest.TestApplication.class)
class BookingJpaRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private BookingJpaRepository bookingJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSaveAndFindBookingById() {
        UserEntity user = entityManager.persistAndFlush(
                new UserEntity(
                        "integration-test@example.com",
                        "hashed-password",
                        UserRole.USER
                )
        );

        BookableServiceEntity service = entityManager.persistAndFlush(
                new BookableServiceEntity(
                        "Java consultation",
                        "Individual Java consultation",
                        60,
                        true
                )
        );

        LocalDateTime startTime =
                LocalDateTime.of(2035, 8, 1, 10, 0);

        LocalDateTime endTime =
                startTime.plusMinutes(60);

        BookingEntity bookingToSave = new BookingEntity(
                user,
                service,
                startTime,
                endTime,
                BookingStatus.PENDING
        );

        BookingEntity savedBooking =
                bookingJpaRepository.saveAndFlush(bookingToSave);

        Long bookingId = savedBooking.getId();

        entityManager.clear();

        BookingEntity foundBooking = bookingJpaRepository
                .findById(bookingId)
                .orElseThrow();

        assertThat(foundBooking.getId())
                .isEqualTo(bookingId);

        assertThat(foundBooking.getStartTime())
                .isEqualTo(startTime);

        assertThat(foundBooking.getEndTime())
                .isEqualTo(endTime);

        assertThat(foundBooking.getStatus())
                .isEqualTo(BookingStatus.PENDING);

        assertThat(foundBooking.getUser().getId())
                .isEqualTo(user.getId());

        assertThat(foundBooking.getUser().getEmail())
                .isEqualTo("integration-test@example.com");

        assertThat(foundBooking.getService().getId())
                .isEqualTo(service.getId());

        assertThat(foundBooking.getService().getName())
                .isEqualTo("Java consultation");
    }

    @Test
    void shouldFindBookingsByStatus() {
        UserEntity user = entityManager.persistAndFlush(
                new UserEntity(
                        "status-test@example.com",
                        "hashed-password",
                        UserRole.USER
                )
        );

        BookableServiceEntity service = entityManager.persistAndFlush(
                new BookableServiceEntity(
                        "Spring consultation",
                        "Individual Spring consultation",
                        60,
                        true
                )
        );

        LocalDateTime startTime =
                LocalDateTime.of(2035, 8, 2, 10, 0);

        BookingEntity pendingBooking = new BookingEntity(
                user,
                service,
                startTime,
                startTime.plusMinutes(60),
                BookingStatus.PENDING
        );

        BookingEntity cancelledBooking = new BookingEntity(
                user,
                service,
                startTime.plusHours(2),
                startTime.plusHours(3),
                BookingStatus.CANCELLED
        );

        bookingJpaRepository.saveAndFlush(pendingBooking);
        bookingJpaRepository.saveAndFlush(cancelledBooking);

        entityManager.clear();

        var pendingBookings =
                bookingJpaRepository.findByStatus(BookingStatus.PENDING);

        assertThat(pendingBookings)
                .hasSize(1);

        assertThat(pendingBookings.get(0).getStatus())
                .isEqualTo(BookingStatus.PENDING);

        assertThat(pendingBookings.get(0).getStartTime())
                .isEqualTo(startTime);
    }

    @Test
    void shouldUpdateBookingStatus() {
        UserEntity user = entityManager.persistAndFlush(
                new UserEntity(
                        "update-status-test@example.com",
                        "hashed-password",
                        UserRole.USER
                )
        );

        BookableServiceEntity service = entityManager.persistAndFlush(
                new BookableServiceEntity(
                        "Database consultation",
                        "Individual database consultation",
                        60,
                        true
                )
        );

        LocalDateTime startTime =
                LocalDateTime.of(2035, 8, 3, 10, 0);

        BookingEntity booking = new BookingEntity(
                user,
                service,
                startTime,
                startTime.plusMinutes(60),
                BookingStatus.PENDING
        );

        BookingEntity savedBooking =
                bookingJpaRepository.saveAndFlush(booking);

        Long bookingId = savedBooking.getId();

        entityManager.clear();

        BookingEntity bookingToUpdate = bookingJpaRepository
                .findById(bookingId)
                .orElseThrow();

        bookingToUpdate.setStatus(BookingStatus.CANCELLED);

        bookingJpaRepository.saveAndFlush(bookingToUpdate);

        entityManager.clear();

        BookingEntity updatedBooking = bookingJpaRepository
                .findById(bookingId)
                .orElseThrow();

        assertThat(updatedBooking.getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void shouldDeleteBookingById() {
        UserEntity user = entityManager.persistAndFlush(
                new UserEntity(
                        "delete-test@example.com",
                        "hashed-password",
                        UserRole.USER
                )
        );

        BookableServiceEntity service = entityManager.persistAndFlush(
                new BookableServiceEntity(
                        "Docker consultation",
                        "Individual Docker consultation",
                        60,
                        true
                )
        );

        LocalDateTime startTime =
                LocalDateTime.of(2035, 8, 4, 10, 0);

        BookingEntity booking = new BookingEntity(
                user,
                service,
                startTime,
                startTime.plusMinutes(60),
                BookingStatus.PENDING
        );

        BookingEntity savedBooking =
                bookingJpaRepository.saveAndFlush(booking);

        Long bookingId = savedBooking.getId();

        bookingJpaRepository.deleteById(bookingId);
        bookingJpaRepository.flush();

        entityManager.clear();

        assertThat(bookingJpaRepository.findById(bookingId))
                .isEmpty();
    }


    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(
            basePackageClasses = {
                    BookingEntity.class,
                    UserEntity.class,
                    BookableServiceEntity.class
            }
    )
    @EnableJpaRepositories(
            basePackageClasses = BookingJpaRepository.class
    )
    static class TestApplication {
    }
}
