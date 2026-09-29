package com.viktor.booking.application.repository;

import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    List<User> findAll();

//    List<User> findByRole(UserRole role);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    User save(User user);
}
