package com.ciphermesh.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Production-ready JWT service for CipherMesh.
 *
 * Security decisions:
 * - Uses HS256 with a secret read from application properties (never hardcoded).
 * - Secret must be at least 256 bits (32 chars) — enforced at startup.
 * - Tokens carry userId (UUID) and username as claims.
 * - Expiry is configurable via properties.
 * - validateToken never throws — returns boolean for safe use in filters.
 * - Sensitive token content is never logged.
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:900000}") long expirationMs) {

        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 characters (256 bits). " +
                "Set app.jwt.secret in application.properties or as an env var.");
        }

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        log.info("JwtService initialized — expiration: {}ms", expirationMs);
    }

    /**
     * Generate a signed JWT for the given user.
     *
     * @param userId   the user's UUID (stored as subject)
     * @param username the user's username (stored as a claim)
     * @return signed JWT string
     */
    public String generateToken(UUID userId, String username) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
        // Security: token content is never logged here
    }

    /**
     * Extract the userId (UUID) from a validated JWT.
     *
     * @param token the JWT string
     * @return UUID of the token owner
     * @throws JwtException if the token is invalid or expired
     */
    public UUID extractUserId(String token) {
        String subject = parseClaims(token).getSubject();
        return UUID.fromString(subject);
    }

    /**
     * Extract the username claim from a validated JWT.
     *
     * @param token the JWT string
     * @return username claim
     */
    public String extractUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    /**
     * Validate the JWT signature and expiry.
     * Returns false instead of throwing — safe for use in security filters.
     *
     * @param token the JWT string
     * @return true if the token is valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException ex) {
            // Security: log class name only — never log the token itself
            log.warn("JWT validation failed: {}", ex.getClass().getSimpleName());
            return false;
        } catch (IllegalArgumentException ex) {
            log.warn("JWT token is null or empty");
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
