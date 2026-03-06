package com.ciphermesh.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

/**
 * DTO for user registration requests.
 * Security note: only public identity data is accepted — no sensitive fields.
 */
@Value
@Builder
public class RegisterUserRequest {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username contains invalid characters")
    String username;

    /**
     * Client's long-term identity PUBLIC key, Base64-encoded.
     * The server stores this as-is — it is public information.
     */
    @NotBlank(message = "Identity public key must not be blank")
    String identityPublicKey;
}
