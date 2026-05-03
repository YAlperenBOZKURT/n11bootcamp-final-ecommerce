package com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component

// Enabled by default. I set it cause of test "RateLimitIT.java" 
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String AUTH_PATH = "/api/users/auth/";
    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 60;
    private static final String RATE_LIMIT_PREFIX = "rl:auth:";


    // Lua script for atomic rate limiting in Redis. Increments the counter and sets expiry on first increment.
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            if current > tonumber(ARGV[2]) then
                return 0
            end
            return 1
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Counter failOpenCounter;

    public RateLimitFilter(StringRedisTemplate stringRedisTemplate,
                           ObjectMapper objectMapper,
                           MeterRegistry meterRegistry) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.failOpenCounter = Counter.builder("rate_limit.fail_open.count")
                .description("Number of requests allowed because Redis rate-limit backend was unavailable")
                .register(meterRegistry);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(AUTH_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = extractClientIp(request);
        String key = RATE_LIMIT_PREFIX + ip;

        Long allowed;
        try {
            allowed = stringRedisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    Collections.singletonList(key),
                    String.valueOf(WINDOW_SECONDS),
                    String.valueOf(MAX_REQUESTS)
            );
        } catch (Exception e) {

            // Fail-open: if Redis is unavailable, keep auth endpoints reachable, it can be a bad choice !!!
            log.warn("Rate limit backend unavailable, allowing request. ip={}, path={}", ip, request.getRequestURI());
            failOpenCounter.increment();
            filterChain.doFilter(request, response);
            return;
        }

        if (!Long.valueOf(0L).equals(allowed)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long retryAfter = null;
        try {
            retryAfter = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("Could not read rate-limit ttl for key={}", key);
        }

        if (retryAfter != null && retryAfter > 0) {
            response.setHeader("Retry-After", String.valueOf(retryAfter));
        }
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("Too many requests. Please wait 1 minute."));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
