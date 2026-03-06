package com.ciphermesh.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter — runs once per request.
 *
 * Security decisions:
 * - Reads token from the Authorization header (Bearer scheme only).
 * - On valid token, populates the SecurityContext so downstream code
 *   can call SecurityContextHolder.getContext().getAuthentication().
 * - On invalid/missing token, the request continues unauthenticated
 *   (Spring Security will reject it at the authorization layer if required).
 * - The raw token string is NEVER logged.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.validateToken(token)) {
            // Security: invalid token — let the request proceed unauthenticated
            // Spring Security will reject it at the authorization layer
            log.debug("Invalid JWT on request to {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Token is valid — build authentication and set on SecurityContext
        try {
            UUID userId   = jwtService.extractUserId(token);
            String username = jwtService.extractUsername(token);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userId,                                    // principal = UUID
                            null,                                      // no credentials after auth
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated user: username={}, path={}", username, request.getRequestURI());

        } catch (Exception ex) {
            // Security: clear context on any extraction error — fail safe
            log.warn("Failed to set authentication from JWT: {}", ex.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract the raw JWT string from the Authorization header.
     * Returns null if the header is absent or not in Bearer format.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
