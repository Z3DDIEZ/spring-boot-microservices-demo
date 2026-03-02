package com.meridian.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object (DTO) capturing the payload for an access token refresh
 * attempt.
 * Expects a non-blank refresh token previously issued by the server.
 */
@Data
public class TokenRefreshRequest {
    @NotBlank
    private String refreshToken;
}
