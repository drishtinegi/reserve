package com.expensetracker.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The opaque random token string stored in the database and sent to the client
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    // Which user this refresh token belongs to
    @Column(nullable = false)
    private Long userId;

    // When this token expires — checked on every /auth/refresh call
    @Column(nullable = false)
    private Instant expiryDate;

    public RefreshToken() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Instant getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Instant expiryDate) { this.expiryDate = expiryDate; }
}
