package com.viktor.booking.infrastructure.security;

import com.viktor.booking.application.exception.InvalidTokenException;
import com.viktor.booking.application.security.TokenService;
import com.viktor.booking.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenService implements TokenService {

    private static final String ISSUER = "booking-system";

    private final SecretKey signingKey;
    private final long expirationMs;
    private final Clock clock;

    public JwtTokenService(
            @Value("${security.jwt.secret}") String base64Secret,
            @Value("${security.jwt.expiration-ms}") long expirationMs
    ) {
        this(
                base64Secret,
                expirationMs,
                Date::new
        );
    }

    JwtTokenService(
            String base64Secret,
            long expirationMs,
            Clock clock
    ) {
        if (expirationMs <= 0) {
            throw new IllegalArgumentException(
                    "JWT expiration must be greater than zero"
            );
        }

        byte[] keyBytes =
                Decoders.BASE64.decode(base64Secret);

        this.signingKey =
                Keys.hmacShaKeyFor(keyBytes);

        this.expirationMs = expirationMs;
        this.clock = clock;
    }

    @Override
    public String generateToken(User user) {
        Date issuedAt = clock.now();

        Date expiration = new Date(
                issuedAt.getTime() + expirationMs
        );

        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getEmail())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(signingKey)
                .compact();
    }

    @Override
    public String extractEmail(String token) {
        try {
            return extractClaims(token)
                    .getSubject();
        } catch (
                JwtException |
                IllegalArgumentException exception
        ) {
            throw new InvalidTokenException(exception);
        }
    }

    @Override
    public boolean isTokenValid(
            String token,
            User user
    ) {
        try {
            String tokenEmail =
                    extractEmail(token);

            return user.getEmail()
                    .equals(tokenEmail);
        } catch (InvalidTokenException exception) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .clock(clock)
                .requireIssuer(ISSUER)
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
