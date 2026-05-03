package com.yabozkurt.n11bootcamp.ecommerce.gateway.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    public boolean isTokenValid(String token) {
        try {
            extractClaim(token, Claims::getSubject);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractUserId(String token) {
        Object userId = extractClaim(token, claims -> claims.get("userId"));
        return userId != null ? userId.toString() : null;
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private SecretKey getSigningKey() {
        return hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
