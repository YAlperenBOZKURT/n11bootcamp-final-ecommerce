package com.yabozkurt.n11bootcamp.ecommerce.user.application.service;

public interface TokenService {

    void blacklistToken(String token, long ttlMs);

    boolean isTokenBlacklisted(String token);

    void storeResetToken(String token, String email, long ttlMs);

    String getResetTokenEmail(String token);

    void deleteResetToken(String token);
}
