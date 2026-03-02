package com.meridian.auth.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object (DTO) returned to the client upon successful
 * authentication.
 * Encapsulates the JWT Access Token, the long-lived Refresh Token, and user
 * metadata
 * to populate frontend state (username, roles).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private String username;
    private List<String> roles;
}
