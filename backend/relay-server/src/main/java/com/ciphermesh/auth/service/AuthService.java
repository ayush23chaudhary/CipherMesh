package com.ciphermesh.auth.service;

import com.ciphermesh.auth.dto.AuthResponse;
import com.ciphermesh.auth.dto.LoginRequest;
import com.ciphermesh.auth.dto.RegisterRequest;
import com.ciphermesh.auth.jwt.JwtService;
import com.ciphermesh.common.exception.ConflictException;
import com.ciphermesh.common.exception.ResourceNotFoundException;
import com.ciphermesh.user.entity.User;
import com.ciphermesh.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication service for CipherMesh.
 *
 * Security decisions:
 * - Anonymous-first: no passwords are used or stored.
 * - JWT is issued immediately after registration or login.
 * - Usernames are unique — duplicate registration is rejected with 409.
 * - Login uses username lookup only; identity key verification is
 *   intentionally deferred to a challenge-response scheme in a future
 *   hardening iteration (flagged with TODO).
 * - No sensitive data (keys, tokens) is written to logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * Register a new user and return a JWT.
     *
     * @param request registration payload (username + identity public key)
     * @return AuthResponse containing the JWT and public user info
     * @throws ConflictException if username is already taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username '" + request.getUsername() + "' is already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .identityPublicKey(request.getIdentityPublicKey())
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: username={}", saved.getUsername());
        // Security: do not log userId or identityPublicKey

        String token = jwtService.generateToken(saved.getId(), saved.getUsername());
        return buildAuthResponse(token, saved);
    }

    /**
     * Authenticate an existing user by username and return a JWT.
     *
     * TODO (hardening): Replace with challenge-response using the stored
     * identityPublicKey — the client should sign a server-issued nonce
     * with their private key to prove ownership, rather than trusting
     * the username alone.
     *
     * @param request login payload (username only)
     * @return AuthResponse containing the JWT and public user info
     * @throws ResourceNotFoundException if the username does not exist
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with username: " + request.getUsername()));

        log.info("User login: username={}", user.getUsername());

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        return buildAuthResponse(token, user);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .accessToken(token)
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }
}
