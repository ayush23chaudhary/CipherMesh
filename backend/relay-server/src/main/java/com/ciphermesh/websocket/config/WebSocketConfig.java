package com.ciphermesh.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for the CipherMesh relay server.
 *
 * Security notes:
 * - All WebSocket connections must be authenticated (JWT enforced at handshake)
 * - Messages relayed by the broker are already encrypted by the client
 * - The server never inspects message payloads
 * - Uses STOMP over WebSocket for structured framing
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker for user-to-user relay
        // Security: messages are opaque ciphertext blobs to the broker
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WSS endpoint — TLS termination handled at load balancer/proxy level
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Restrict in production via config
                .withSockJS();
    }
}
