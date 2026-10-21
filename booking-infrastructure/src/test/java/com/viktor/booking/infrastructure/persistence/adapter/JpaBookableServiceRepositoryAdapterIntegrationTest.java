package com.viktor.booking.infrastructure.persistence.adapter;

import com.viktor.booking.domain.model.BookableService;
import com.viktor.booking.infrastructure.persistence.entity.BookableServiceEntity;
import com.viktor.booking.infrastructure.persistence.mapper.BookableServiceMapper;
import com.viktor.booking.infrastructure.persistence.repository.BookableServiceJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("jpa")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ContextConfiguration(
        classes =
                JpaBookableServiceRepositoryAdapterIntegrationTest
                        .TestConfiguration.class
)
class JpaBookableServiceRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JpaBookableServiceRepositoryAdapter serviceRepositoryAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSaveAndFindServiceThroughAdapter() {
        BookableService serviceToSave = new BookableService(
                null,
                "Backend consultation",
                "Individual backend consultation",
                60,
                true
        );

        BookableService savedService =
                serviceRepositoryAdapter.save(serviceToSave);

        assertThat(savedService.getId())
                .isNotNull();

        entityManager.flush();
        entityManager.clear();

        BookableService foundService = serviceRepositoryAdapter
                .findById(savedService.getId())
                .orElseThrow();

        assertThat(foundService.getId())
                .isEqualTo(savedService.getId());

        assertThat(foundService.getName())
                .isEqualTo("Backend consultation");

        assertThat(foundService.getDescription())
                .isEqualTo("Individual backend consultation");

        assertThat(foundService.getDurationMinutes())
                .isEqualTo(60);

        assertThat(foundService.isActive())
                .isTrue();
    }

    @Test
    void shouldFindAllServicesThroughAdapter() {

        BookableService firstService =
                serviceRepositoryAdapter.save(
                        new BookableService(
                                null,
                                "Java consultation",
                                "Individual Java consultation",
                                60,
                                true
                        )
                );

        BookableService secondService =
                serviceRepositoryAdapter.save(
                        new BookableService(
                                null,
                                "Docker consultation",
                                "Individual Docker consultation",
                                45,
                                false
                        )
                );

        entityManager.flush();
        entityManager.clear();

        var services = serviceRepositoryAdapter.findAll();

        assertThat(services)
                .extracting(BookableService::getId)
                .contains(
                        firstService.getId(),
                        secondService.getId()
                );

        assertThat(services)
                .filteredOn(service ->
                        service.getId().equals(firstService.getId())
                                || service.getId().equals(secondService.getId())
                )
                .extracting(BookableService::getName)
                .containsExactlyInAnyOrder(
                        "Java consultation",
                        "Docker consultation"
                );
    }


    @Test
    void shouldUpdateActiveStateThroughAdapter() {
        BookableService savedService =
                serviceRepositoryAdapter.save(
                        new BookableService(
                                null,
                                "Spring consultation",
                                "Individual Spring consultation",
                                60,
                                true
                        )
                );

        entityManager.flush();
        entityManager.clear();

        BookableService deactivatedService =
                serviceRepositoryAdapter
                        .updateActive(
                                savedService.getId(),
                                false
                        )
                        .orElseThrow();

        assertThat(deactivatedService.isActive())
                .isFalse();

        entityManager.flush();
        entityManager.clear();

        BookableService serviceFromDatabase =
                serviceRepositoryAdapter
                        .findById(savedService.getId())
                        .orElseThrow();

        assertThat(serviceFromDatabase.isActive())
                .isFalse();
    }

    @Test
    void shouldReturnEmptyWhenServiceDoesNotExist() {
        var result =
                serviceRepositoryAdapter.findById(999999L);

        assertThat(result)
                .isEmpty();
    }

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(
            basePackageClasses = BookableServiceEntity.class
    )
    @EnableJpaRepositories(
            basePackageClasses =
                    BookableServiceJpaRepository.class
    )
    @Import({
            JpaBookableServiceRepositoryAdapter.class,
            BookableServiceMapper.class
    })
    static class TestConfiguration {
    }
}
