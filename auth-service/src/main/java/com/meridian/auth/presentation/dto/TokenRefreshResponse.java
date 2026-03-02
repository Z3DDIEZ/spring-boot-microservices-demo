package com.meridian.auth.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object (DTO) returned to the client upon a successful token
 * refresh.
 * Provides the newly minted JWT Access Token. The existing refresh token is
 * typically
 * echoed back, though token rotation strategies may update it.
 */
@Data
@AllArgsConstructor
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    public TokenRefreshResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
