package com.ciphermesh.auth.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Response DTO returned after successful registration or login.
 *
 * Security note:
 * - Only the access token and minimal public user data are returned.
 * - The token itself is opaque to the client (signed HS256 JWT).
 * - Never include sensitive fields (private keys, internal IDs beyond UUID).
 */
@Value
@Builder
public class AuthResponse {

    /** Signed JWT access token. */
    String accessToken;

    /** Token type, always "Bearer". */
    @Builder.Default
    String tokenType = "Bearer";

    /** User's UUID — safe to expose as a public identifier. */
    UUID userId;

    /** User's chosen username. */
    String username;
}
