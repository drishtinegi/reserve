package com.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor
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
}
