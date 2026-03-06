package com.ciphermesh.auth.controller;

import com.ciphermesh.auth.dto.AuthResponse;
import com.ciphermesh.auth.dto.LoginRequest;
import com.ciphermesh.auth.dto.RegisterRequest;
import com.ciphermesh.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 *
 * Base path: /api/auth
 *
 * Security notes:
 * - All inputs validated via Bean Validation (@Valid).
 * - Tokens are never logged.
 * - Endpoints /api/auth/** are publicly accessible (see SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     *
     * Register a new user (anonymous-first — no password required).
     * Returns a JWT on success.
     *
     * @param request { username, identityPublicKey }
     * @return 201 Created + AuthResponse
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for username={}", request.getUsername());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     *
     * Authenticate by username and receive a JWT.
     *
     * @param request { username }
     * @return 200 OK + AuthResponse
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for username={}", request.getUsername());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
