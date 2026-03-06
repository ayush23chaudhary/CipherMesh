package com.ciphermesh.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

/**
 * Request DTO for login.
 *
 * Security note:
 * - CipherMesh is anonymous-first — authentication is username-only.
 * - The server trusts username + identity key, not a password.
 * - In a hardened version this would be replaced with a challenge-response
 *   scheme using the identity key pair (future iteration).
 */
@Value
public class LoginRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 64, message = "Username must be 3–64 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_.-]+$",
        message = "Username may only contain letters, digits, underscores, dots, or hyphens"
    )
    String username;
}
