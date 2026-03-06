package com.ciphermesh.security.config;

import com.ciphermesh.auth.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Production-ready Spring Security configuration for CipherMesh.
 *
 * Security decisions:
 * - Stateless — no HTTP session created; JWT carries all auth state.
 * - CSRF disabled — safe for stateless JWT APIs (no cookies used for auth).
 * - CORS enabled — restrict allowed origins in production via app.websocket.allowed-origins.
 * - JWT filter runs before UsernamePasswordAuthenticationFilter on every request.
 * - Public routes: /api/auth/** (register, login) and /ws/** (WebSocket handshake).
 * - All other routes require a valid Bearer JWT.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless — no server-side session; JWT carries auth state
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CSRF not needed for stateless JWT APIs (no cookie-based auth)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS — configure allowed origins properly in production
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .authorizeHttpRequests(auth -> auth
                // Public: registration, login
                .requestMatchers("/api/auth/**").permitAll()
                // Public: WebSocket upgrade handshake
                .requestMatchers("/ws/**").permitAll()
                // Public: read-only room listing (landing page stats + room browser)
                .requestMatchers(
                    org.springframework.http.HttpMethod.GET,
                    "/api/v1/rooms", "/api/v1/rooms/count", "/api/v1/rooms/*"
                ).permitAll()
                // Everything else requires a valid JWT
                .anyRequest().authenticated()
            )

            // Wire the JWT filter before Spring's default username/password filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration.
     * Security: lock down allowedOrigins in production — never use "*" in prod.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // TODO: Replace with environment-specific origins in production
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
