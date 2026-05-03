package com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.redis;

import com.yabozkurt.n11bootcamp.ecommerce.user.application.service.TokenService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisTokenService implements TokenService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisTokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklistToken(String token, long ttlMs) {
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", ttlMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    private static final String RESET_PREFIX = "reset:";

    @Override
    public void storeResetToken(String token, String email, long ttlMs) {
        redisTemplate.opsForValue().set(RESET_PREFIX + token, email, ttlMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getResetTokenEmail(String token) {
        return redisTemplate.opsForValue().get(RESET_PREFIX + token);
    }

    @Override
    public void deleteResetToken(String token) {
        redisTemplate.delete(RESET_PREFIX + token);
    }
}
