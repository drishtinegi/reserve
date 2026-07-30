package com.expensetracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JwtService — the only class that knows how to create and read JWT tokens.
 *
 * A JWT (JSON Web Token) is a string in three dot-separated parts:
 *   header.payload.signature
 *
 * The payload holds the username and expiry. The signature is a cryptographic
 * hash that proves the token was issued by us (using our secret key) and
 * hasn't been tampered with. We never store tokens in the database — the
 * signature itself is the proof of validity.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        // Key must be at least 256 bits (32 bytes) for HMAC-SHA256
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Creates a signed JWT token containing the username as the subject. */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    /** Extracts the username from a token (throws if token is invalid or expired). */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Returns true if the token is valid and not yet expired. */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
