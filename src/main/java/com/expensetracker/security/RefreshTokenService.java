package com.expensetracker.security;

import com.expensetracker.entity.RefreshToken;
import com.expensetracker.exception.ResourceNotFoundException;
import com.expensetracker.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * RefreshTokenService — manages the lifecycle of refresh tokens.
 *
 * Responsibilities:
 *   generate()  — create and persist a new refresh token for a user
 *   validate()  — check that a token exists in the DB and hasn't expired
 *   deleteByUserId() — wipe all tokens for a user (used on logout)
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    /**
     * Creates a new refresh token for the given userId and saves it to the DB.
     * Uses UUID as the token value — random, unique, and unguessable.
     */
    public RefreshToken generate(Long userId) {
        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiryDate(Instant.now().plus(refreshExpirationDays, ChronoUnit.DAYS));
        return refreshTokenRepository.save(rt);
    }

    /**
     * Looks up a token string in the DB.
     * Throws 404 if not found (token never existed or already deleted by logout).
     * Throws IllegalArgumentException if found but expired.
     * Returns the RefreshToken entity if valid.
     */
    public RefreshToken validate(String tokenValue) {
        RefreshToken rt = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Refresh token not found — please log in again."));

        if (rt.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(rt); // clean up expired token
            throw new IllegalArgumentException(
                    "Refresh token has expired — please log in again.");
        }

        return rt;
    }

    /**
     * Deletes all refresh tokens for a user.
     * Called on logout so the user's sessions are fully invalidated.
     */
    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
