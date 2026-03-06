package com.ciphermesh.keys.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PreKey controller — handles Signal-style prekey bundle upload and retrieval.
 *
 * Security note:
 * - Only PUBLIC prekeys are stored here.
 * - Private keys must NEVER be sent to or stored on the server.
 *
 * TODO: Implement prekey bundle upload, one-time prekey fetch endpoints.
 */
@RestController
@RequestMapping("/api/v1/keys")
public class KeyController {
    // Stub — to be implemented with keys module
}
