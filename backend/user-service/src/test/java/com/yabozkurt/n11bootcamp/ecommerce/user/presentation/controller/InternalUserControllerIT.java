package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.UserRepository;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.AddressRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.LoginRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.messaging.publisher.UserEventPublisher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=optional:configserver:"
        }
)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class InternalUserControllerIT {

    @MockBean
    UserEventPublisher userEventPublisher;

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

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void getInternalUser_existingUser_returnsData() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("internal@test.com", "Test123!");

        String profileJson = mockMvc.perform(get("/api/users/me").cookie(cookie))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(profileJson).path("data").path("id").asLong();

        mockMvc.perform(get("/api/users/internal/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("internal@test.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.defaultAddress").isEmpty());
    }

    @Test
    void getInternalUser_withDefaultAddress_returnsAddress() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("withaddr@test.com", "Test123!");

        AddressRequest addr = new AddressRequest();
        addr.setTitle("Ev");
        addr.setRecipientName("Test User");
        addr.setRecipientPhone("5551234567");
        addr.setCity("Ankara");
        addr.setDistrict("Çankaya");
        addr.setAddressLine("Test Caddesi No:5");

        mockMvc.perform(post("/api/users/me/addresses")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addr)))
                .andExpect(status().isOk());

        String profileJson = mockMvc.perform(get("/api/users/me").cookie(cookie))
                .andReturn().getResponse().getContentAsString();
        Long userId = objectMapper.readTree(profileJson).path("data").path("id").asLong();

        mockMvc.perform(get("/api/users/internal/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.defaultAddress.city").value("Ankara"))
                .andExpect(jsonPath("$.data.defaultAddress.default").value(true));
    }

    @Test
    void getInternalUser_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/users/internal/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getInternalUser_noAuthRequired_accessible() throws Exception {
        mockMvc.perform(get("/api/users/internal/1"))
                .andExpect(status().isNotFound()); // 404 not 401 — no auth needed
    }

    // -- helpers ---------------------------------------------------------------

    private Cookie loginAndGetAccessCookie(String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setFirstName("Test");
        reg.setLastName("User");
        reg.setPhoneNumber("5550000000");

        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("access_token");
    }
}
