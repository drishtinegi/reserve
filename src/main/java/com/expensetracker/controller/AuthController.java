package com.expensetracker.controller;

import com.expensetracker.dto.AuthDtos.*;
import com.expensetracker.entity.RefreshToken;
import com.expensetracker.entity.User;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.JwtService;
import com.expensetracker.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Register, log in, refresh access tokens, and log out")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Operation(summary = "Register a new user — returns a short-lived access token and a refresh token")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new IllegalArgumentException("Username already taken: " + req.username());
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setEmail(req.email());
        user.setPreferredCurrency(req.preferredCurrency());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.generate(user.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(accessToken, refreshToken.getToken(), user.getUsername()));
    }

    @Operation(summary = "Log in — returns a short-lived access token (15 min) and a refresh token (7 days)")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        String accessToken = jwtService.generateToken(user.getUsername());
        RefreshToken refreshToken = refreshTokenService.generate(user.getId());

        return ResponseEntity.ok(
                new AuthResponse(accessToken, refreshToken.getToken(), user.getUsername()));
    }

    @Operation(summary = "Exchange a valid refresh token for a new short-lived access token")
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        // validate() throws if the token is missing or expired
        RefreshToken rt = refreshTokenService.validate(req.refreshToken());

        // Look up the user the refresh token belongs to
        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        String newAccessToken = jwtService.generateToken(user.getUsername());
        return ResponseEntity.ok(new AccessTokenResponse(newAccessToken));
    }

    @Operation(summary = "Log out — deletes all refresh tokens for the logged-in user")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        refreshTokenService.deleteByUserId(user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get the current user's username and preferredCurrency")
    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        return ResponseEntity.ok(new MeResponse(user.getUsername(), user.getPreferredCurrency()));
    }
}
