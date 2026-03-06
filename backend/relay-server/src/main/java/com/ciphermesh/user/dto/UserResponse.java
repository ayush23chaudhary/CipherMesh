package com.ciphermesh.user.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for user profile data.
 * Only exposes public, non-sensitive fields.
 */
@Value
@Builder
public class UserResponse {
    UUID id;
    String username;
    String identityPublicKey;
    Instant createdAt;
}
