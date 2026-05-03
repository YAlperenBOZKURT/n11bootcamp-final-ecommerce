package com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.security.filter;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.TokenService;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.security.jwt.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final TokenService tokenService;

    public JwtAuthFilter(JwtService jwtService, TokenService tokenService) {
        this.jwtService = jwtService;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Read JWT from Authorization header first, then fallback to access_token cookie
        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Blacklisted token -> do not authenticate, continue filter chain
        if (tokenService.isTokenBlacklisted(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            // Only access tokens are valid for request authentication
            if (jwtService.isRefreshToken(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String email = jwtService.extractEmail(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Build authorities directly from JWT claims to avoid an extra DB lookup
                String role = jwtService.extractRole(token);
                var authorities = role != null
                        ? Collections.singletonList(new SimpleGrantedAuthority(role))
                        : Collections.<SimpleGrantedAuthority>emptyList();

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT, skipping authentication: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Unexpected error while processing JWT auth filter: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    // Extract token from Bearer header first; if absent, try access_token cookie
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
