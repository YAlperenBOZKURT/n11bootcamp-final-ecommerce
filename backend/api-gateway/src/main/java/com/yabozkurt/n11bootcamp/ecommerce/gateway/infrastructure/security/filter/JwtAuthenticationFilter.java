package com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.filter;

import com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.config.GatewaySecurityProperties;
import com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.jwt.JwtService;
import com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.web.filter.MutableHttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    private final JwtService jwtService;
    private final GatewaySecurityProperties securityProperties;

    public JwtAuthenticationFilter(JwtService jwtService, GatewaySecurityProperties securityProperties) {
        this.jwtService = jwtService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        if (matchesAdminEndpoint(request.getMethod(), path)) {
            return false;
        }

        boolean isPublic = securityProperties.getPublicEndpoints().stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
        if (isPublic) return true;

        if (HttpMethod.GET.matches(request.getMethod())) {
            boolean isPublicGet = securityProperties.getPublicGetEndpoints().stream()
                    .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
            return isPublicGet;
        }

        return false;
    }

    private boolean matchesAdminEndpoint(String method, String path) {
        for (String rule : securityProperties.getAdminEndpoints()) {
            if (rule == null || rule.isBlank() || !rule.contains(":")) {
                continue;
            }
            String[] parts = rule.split(":", 2);
            String ruleMethod = parts[0].trim().toUpperCase(Locale.ROOT);
            String rulePath = parts[1].trim();
            if (ruleMethod.equalsIgnoreCase(method) && PATH_MATCHER.match(rulePath, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token == null || token.isBlank()) {
            unauthorized(response, "Missing token");
            return;
        }

        try {
            if (!jwtService.isTokenValid(token)) {
                unauthorized(response, "Invalid token");
                return;
            }

            // Reject refresh tokens — only access tokens allowed here
            String type = jwtService.extractType(token);
            if (!"access".equals(type)) {
                unauthorized(response, "Invalid token type");
                return;
            }

            String email = jwtService.extractEmail(token);
            if (email == null || email.isBlank()) {
                unauthorized(response, "Invalid token subject");
                return;
            }

            String role = jwtService.extractRole(token);
            String userId = jwtService.extractUserId(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            role != null ? Collections.singletonList(new SimpleGrantedAuthority(role)) : Collections.emptyList()
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Forward user context to downstream services
            MutableHttpServletRequest enrichedRequest = new MutableHttpServletRequest(request);
            enrichedRequest.putHeader(HEADER_USER_EMAIL, email);
            if (userId != null) enrichedRequest.putHeader(HEADER_USER_ID, userId);
            if (role != null) enrichedRequest.putHeader(HEADER_USER_ROLE, role);

            filterChain.doFilter(enrichedRequest, response);
        } catch (Exception ex) {
            unauthorized(response, "Invalid token");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if ("access_token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
