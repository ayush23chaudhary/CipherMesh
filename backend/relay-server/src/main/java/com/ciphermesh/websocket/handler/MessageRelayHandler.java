package com.ciphermesh.websocket.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket message relay handler.
 *
 * Security note:
 * - This handler only relays encrypted payloads between clients.
 * - The server NEVER reads or decrypts message content.
 * - Messages are opaque Base64 ciphertext blobs to this handler.
 *
 * TODO: Add message size limits, rate limiting, recipient validation.
 */
@Controller
@Slf4j
public class MessageRelayHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public MessageRelayHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Relay an encrypted message to the intended recipient.
     * The payload is treated as opaque — never inspected or logged.
     *
     * @param payload   Encrypted message payload (opaque to server)
     * @param principal Authenticated sender
     */
    @MessageMapping("/message.send")
    public void relayMessage(@Payload String payload, Principal principal) {
        // Security: never log payload content — it may contain encrypted data
        log.debug("Relaying message from sender: {}", principal.getName());

        // TODO: Extract recipient from envelope header (not from plaintext)
        // and route to /user/{recipientId}/queue/messages
    }
}
