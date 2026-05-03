package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.UserRepository;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.ChangePasswordRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.LoginRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.RegisterRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.UpdateProfileRequest;
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
class ProfileControllerIT {

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
    void getProfile_authenticated_returnsProfile() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("profile@test.com", "Test123!");

        mockMvc.perform(get("/api/users/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("profile@test.com"))
                .andExpect(jsonPath("$.data.firstName").value("Test"));
    }

    @Test
    void getProfile_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_validRequest_returnsUpdated() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("update@test.com", "Test123!");

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFirstName("Ahmet");
        req.setLastName("Yılmaz");
        req.setPhoneNumber("5559999999");

        mockMvc.perform(put("/api/users/me")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Ahmet"))
                .andExpect(jsonPath("$.data.lastName").value("Yılmaz"));
    }

    @Test
    void updateProfile_missingFirstName_returns400() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("invalid@test.com", "Test123!");

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setLastName("Yılmaz");

        mockMvc.perform(put("/api/users/me")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_correctCurrent_succeeds() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("changepw@test.com", "Test123!");

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("Test123!");
        req.setNewPassword("NewPass456!");

        mockMvc.perform(put("/api/users/me/password")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void changePassword_wrongCurrent_returns401() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("wrongpw@test.com", "Test123!");

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("WrongPass!");
        req.setNewPassword("NewPass456!");

        mockMvc.perform(put("/api/users/me/password")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteOwnAccount_authenticated_succeeds() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("delete@test.com", "Test123!");

        mockMvc.perform(delete("/api/users/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // After deletion, profile should be gone
        mockMvc.perform(get("/api/users/me").cookie(cookie))
                .andExpect(status().isNotFound());
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
