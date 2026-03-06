package com.ciphermesh.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * User entity — stores only non-sensitive, publicly shareable identity data.
 *
 * Security note:
 * - No passwords are stored here. Authentication credentials live in the auth module.
 * - identityPublicKey is the user's long-term public key (Signal-style).
 *   The private key NEVER leaves the client device.
 * - No message content is ever associated with this entity.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_username", columnList = "username", unique = true),
        @Index(name = "idx_user_identity_key", columnList = "identityPublicKey")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"identityPublicKey"}) // Never accidentally log the public key
public class User {

    /**
     * UUID primary key — never expose sequential IDs to clients.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    /**
     * Unique human-readable handle. Does not expose internal IDs.
     */
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /**
     * The user's long-term identity public key (Base64-encoded).
     * This is a PUBLIC key — safe to store on the server.
     * The corresponding private key must NEVER leave the client.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String identityPublicKey;

    /**
     * Account creation timestamp, set automatically by JPA auditing.
     * Stored as UTC epoch for consistency across timezones.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
