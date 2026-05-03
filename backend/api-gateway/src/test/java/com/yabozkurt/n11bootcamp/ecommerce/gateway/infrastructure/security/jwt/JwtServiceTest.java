package com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "ecommerce-secret-key-n11bootcamp-yabozkurt-2025";
    private static final String OTHER_SECRET = "other-secret-key-n11bootcamp-yabozkurt-2025-xx";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = buildToken("user@test.com", "access", "ROLE_CUSTOMER", 42L, 60_000);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        String token = buildToken("user@test.com", "access", "ROLE_CUSTOMER", 42L, -1000);
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_wrongSecret_returnsFalse() {
        String token = buildTokenWithSecret("user@test.com", "access", "ROLE_CUSTOMER", 42L, 60_000, OTHER_SECRET);
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void extractEmail_returnsSubject() {
        String token = buildToken("ali@test.com", "access", "ROLE_CUSTOMER", 1L, 60_000);
        assertThat(jwtService.extractEmail(token)).isEqualTo("ali@test.com");
    }

    @Test
    void extractType_accessToken_returnsAccess() {
        String token = buildToken("user@test.com", "access", "ROLE_CUSTOMER", 1L, 60_000);
        assertThat(jwtService.extractType(token)).isEqualTo("access");
    }

    @Test
    void extractType_refreshToken_returnsRefresh() {
        String token = buildToken("user@test.com", "refresh", null, null, 60_000);
        assertThat(jwtService.extractType(token)).isEqualTo("refresh");
    }

    @Test
    void extractRole_returnsRole() {
        String token = buildToken("user@test.com", "access", "ROLE_ADMIN", 5L, 60_000);
        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void extractUserId_returnsId() {
        String token = buildToken("user@test.com", "access", "ROLE_CUSTOMER", 99L, 60_000);
        assertThat(jwtService.extractUserId(token)).isEqualTo("99");
    }

    // -- helpers --------------------------------------------------------------

    private String buildToken(String subject, String type, String role, Long userId, long expiryMs) {
        return buildTokenWithSecret(subject, type, role, userId, expiryMs, SECRET);
    }

    private String buildTokenWithSecret(String subject, String type, String role,
                                         Long userId, long expiryMs, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .claim("type", type);

        if (role != null) builder.claim("role", role);
        if (userId != null) builder.claim("userId", userId);

        return builder.signWith(key).compact();
    }
}
