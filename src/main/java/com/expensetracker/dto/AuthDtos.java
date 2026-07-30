package com.expensetracker.dto;

import com.expensetracker.entity.Currency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AuthDtos {

    // ── Incoming: what the client sends when registering ────────────────────
    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank String password,
            @Email @NotBlank String email,
            @NotNull Currency preferredCurrency
    ) {}

    // ── Incoming: what the client sends when logging in ─────────────────────
    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    // ── Incoming: the refresh token the client sends to get a new access token
    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {}

    // ── Outgoing: login/register response — access token + refresh token ─────
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String username
    ) {}

    // ── Outgoing: a new access token issued after a valid refresh ────────────
    public record AccessTokenResponse(
            String accessToken
    ) {}

    // ── Outgoing: current user's profile info ────────────────────────────────
    public record MeResponse(
            String username,
            Currency preferredCurrency
    ) {}
}
