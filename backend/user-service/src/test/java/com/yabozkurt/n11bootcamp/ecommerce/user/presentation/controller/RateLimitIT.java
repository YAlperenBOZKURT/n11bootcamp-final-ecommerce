package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.controller;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.messaging.publisher.UserEventPublisher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=optional:configserver:",
                "rate-limit.enabled=true"
        }
)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Disabled("Run manually: ./mvnw test -Dtest=RateLimitIT")
class RateLimitIT {

    @MockBean
    UserEventPublisher userEventPublisher;

    private static final String TEST_IP = "1.2.3.4";
    private static final String RATE_LIMIT_KEY = "rl:auth:" + TEST_IP;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("user_service_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
        registry.add("DATASOURCE_USERNAME", postgres::getUsername);
        registry.add("DATASOURCE_PASSWORD", postgres::getPassword);
        registry.add("REDIS_HOST", redis::getHost);
        registry.add("REDIS_PORT", () -> redis.getMappedPort(6379));
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void clearRateLimitKey() {
        stringRedisTemplate.delete(RATE_LIMIT_KEY);
    }

    @Test
    void sameIp_exceedsThreshold_returns429() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/users/auth/refresh")
                            .header("X-Forwarded-For", TEST_IP))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/users/auth/refresh")
                        .header("X-Forwarded-For", TEST_IP))
                .andExpect(status().isTooManyRequests());
    }
}
