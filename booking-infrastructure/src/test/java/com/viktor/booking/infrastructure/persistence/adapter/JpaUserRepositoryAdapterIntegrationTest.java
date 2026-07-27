package com.viktor.booking.infrastructure.persistence.adapter;

import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;
import com.viktor.booking.infrastructure.persistence.entity.UserEntity;
import com.viktor.booking.infrastructure.persistence.mapper.UserMapper;
import com.viktor.booking.infrastructure.persistence.repository.UserJpaRepository;
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
        classes = JpaUserRepositoryAdapterIntegrationTest.TestConfiguration.class
)
class JpaUserRepositoryAdapterIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private JpaUserRepositoryAdapter userRepositoryAdapter;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSaveAndFindUserThroughAdapter() {
        User userToSave = new User(
                null,
                "user-adapter-test@example.com",
                "hashed-password",
                UserRole.USER
        );

        User savedUser =
                userRepositoryAdapter.save(userToSave);

        assertThat(savedUser.getId())
                .isNotNull();

        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepositoryAdapter
                .findById(savedUser.getId())
                .orElseThrow();

        assertThat(foundUser.getId())
                .isEqualTo(savedUser.getId());

        assertThat(foundUser.getEmail())
                .isEqualTo("user-adapter-test@example.com");

        assertThat(foundUser.getPasswordHash())
                .isEqualTo("hashed-password");

        assertThat(foundUser.getRole())
                .isEqualTo(UserRole.USER);
    }

    @Test
    void shouldFindUserByEmail() {
        User userToSave = new User(
                null,
                "find-by-email@example.com",
                "hashed-password",
                UserRole.USER
        );

        User savedUser =
                userRepositoryAdapter.save(userToSave);

        entityManager.flush();
        entityManager.clear();

        User foundUser = userRepositoryAdapter
                .findByEmail("find-by-email@example.com")
                .orElseThrow();

        assertThat(foundUser.getId())
                .isEqualTo(savedUser.getId());

        assertThat(foundUser.getEmail())
                .isEqualTo("find-by-email@example.com");

        assertThat(foundUser.getPasswordHash())
                .isEqualTo("hashed-password");

        assertThat(foundUser.getRole())
                .isEqualTo(UserRole.USER);
    }

    @Test
    void shouldReturnEmptyWhenUserEmailDoesNotExist() {
        var result = userRepositoryAdapter.findByEmail(
                "missing-user@example.com"
        );

        assertThat(result)
                .isEmpty();
    }

    @Test
    void shouldReturnTrueWhenUserEmailExists() {
        User userToSave = new User(
                null,
                "existing-email@example.com",
                "hashed-password",
                UserRole.USER
        );

        userRepositoryAdapter.save(userToSave);

        entityManager.flush();
        entityManager.clear();

        boolean result = userRepositoryAdapter.existsByEmail(
                "existing-email@example.com"
        );

        assertThat(result)
                .isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserEmailDoesNotExist() {
        boolean result = userRepositoryAdapter.existsByEmail(
                "not-existing-email@example.com"
        );

        assertThat(result)
                .isFalse();
    }

    @Test
    void shouldReturnEmptyWhenUserDoesNotExist() {
        var result =
                userRepositoryAdapter.findById(999999L);

        assertThat(result)
                .isEmpty();
    }

    @Configuration
    @EnableAutoConfiguration
    @EntityScan(
            basePackageClasses = UserEntity.class
    )
    @EnableJpaRepositories(
            basePackageClasses = UserJpaRepository.class
    )
    @Import({
            JpaUserRepositoryAdapter.class,
            UserMapper.class
    })
    static class TestConfiguration {
    }
}
