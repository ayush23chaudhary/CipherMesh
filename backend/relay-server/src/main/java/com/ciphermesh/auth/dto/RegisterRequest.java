package com.ciphermesh.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

/**
 * Request DTO for user registration.
 *
 * Security note:
 * - CipherMesh is anonymous-first: NO passwords are used.
 * - identityPublicKey is the user's long-term PUBLIC key only.
 *   The private key must NEVER be sent to the server.
 */
@Value
public class RegisterRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 64, message = "Username must be 3–64 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_.-]+$",
        message = "Username may only contain letters, digits, underscores, dots, or hyphens"
    )
    String username;

    /**
     * Base64-encoded long-term identity public key (Signal-style).
     * This is a PUBLIC key — safe to store. The private key stays on the device.
     */
    @NotBlank(message = "Identity public key must not be blank")
    String identityPublicKey;
}
