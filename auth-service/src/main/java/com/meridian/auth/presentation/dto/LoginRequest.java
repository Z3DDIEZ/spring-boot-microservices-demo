package com.meridian.auth.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object (DTO) capturing the credentials payload from a client
 * attempting to authenticate. Validated via Java Bean Validation API.
 */
@Data
public class LoginRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
