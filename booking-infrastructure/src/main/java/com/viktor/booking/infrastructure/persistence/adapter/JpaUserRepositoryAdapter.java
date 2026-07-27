package com.viktor.booking.infrastructure.persistence.adapter;

import com.viktor.booking.application.repository.UserRepository;
import com.viktor.booking.domain.model.User;
import com.viktor.booking.infrastructure.persistence.entity.UserEntity;
import com.viktor.booking.infrastructure.persistence.mapper.UserMapper;
import com.viktor.booking.infrastructure.persistence.repository.UserJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Profile("jpa")
@Transactional
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    public JpaUserRepositoryAdapter(
            UserJpaRepository userJpaRepository,
            UserMapper userMapper
    ) {
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserEntity entity = userMapper.toNewEntity(user);
        UserEntity savedEntity = userJpaRepository.save(entity);

        return userMapper.toDomain(savedEntity);
    }
}