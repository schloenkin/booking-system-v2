package com.viktor.booking.application.service;

import com.viktor.booking.application.dto.auth.AuthResult;
import com.viktor.booking.application.exception.InvalidCredentialsException;
import com.viktor.booking.application.exception.UserAlreadyExistsException;
import com.viktor.booking.application.repository.UserRepository;
import com.viktor.booking.application.security.PasswordHasher;
import com.viktor.booking.application.security.TokenService;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.domain.model.User;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    public AuthResult register(
            String email,
            String rawPassword
    ) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new UserAlreadyExistsException(normalizedEmail);
        }

        String passwordHash =
                passwordHasher.hash(rawPassword);

        User userToSave = new User(
                null,
                normalizedEmail,
                passwordHash,
                UserRole.USER
        );

        User savedUser =
                userRepository.save(userToSave);

        String accessToken =
                tokenService.generateToken(savedUser);

        return new AuthResult(
                accessToken,
                savedUser
        );
    }

    public AuthResult login(
            String email,
            String rawPassword
    ) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);

        boolean passwordMatches = passwordHasher.matches(
                rawPassword,
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new InvalidCredentialsException();
        }

        String accessToken =
                tokenService.generateToken(user);

        return new AuthResult(
                accessToken,
                user
        );
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
