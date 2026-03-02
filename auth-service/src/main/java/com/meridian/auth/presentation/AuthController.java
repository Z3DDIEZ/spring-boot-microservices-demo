package com.meridian.auth.presentation;

import com.meridian.auth.application.AuthService;
import com.meridian.auth.presentation.dto.AuthResponse;
import com.meridian.auth.presentation.dto.LoginRequest;
import com.meridian.auth.presentation.dto.RegisterRequest;
import com.meridian.auth.presentation.dto.TokenRefreshRequest;
import com.meridian.auth.presentation.dto.TokenRefreshResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller acting as the public-facing Presentation Layer for Identity
 * and Access Management.
 * <p>
 * Exposes endpoints for registering new accounts, exchanging credentials for
 * tokens (login),
 * and seamlessly exchanging refresh tokens for new access tokens.
 * Mapped to {@code /api/v1/auth}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Handles public registration requests for new users.
     *
     * @param registerRequest The validated payload containing desired username,
     *                        email, and password.
     * @return HTTP 201 Created on success, or HTTP 400 Bad Request if the
     *         username/email is already taken.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.registerUser(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Exchanges valid user credentials (username/password) for a short-lived JWT
     * Access Token
     * and a long-lived Refresh Token.
     *
     * @param loginRequest The validated payload containing login credentials.
     * @return HTTP 200 OK containing the tokens, or an implicit HTTP 401
     *         Unauthorized via Spring Security filters.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Issues a new JWT Access Token in exchange for a valid, unexpired Refresh
     * Token.
     * This avoids prompting the user to repeatedly log in.
     *
     * @param request The validated payload containing the active Refresh Token
     *                string.
     * @return HTTP 200 OK containing the new Access Token, or HTTP 403 Forbidden if
     *         the token is expired/invalid.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        try {
            TokenRefreshResponse response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
