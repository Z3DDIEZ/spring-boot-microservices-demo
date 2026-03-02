package com.meridian.auth.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data Transfer Object (DTO) capturing the payload for creating a new user
 * account.
 * Implements strict input sanitization constraints (lengths, email formatting)
 * via
 * Java Bean Validation API to prevent malformed data from reaching the
 * Application Layer.
 */
@Data
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(max = 100)
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;
}
