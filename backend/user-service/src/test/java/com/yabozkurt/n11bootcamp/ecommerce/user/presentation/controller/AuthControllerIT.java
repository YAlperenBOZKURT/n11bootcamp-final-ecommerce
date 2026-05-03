package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AuthControllerIT {

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

    // --- register ------------------------------------------------------------

    @Test
    void register_success_returnsCookiesAndBody() throws Exception {
        RegisterRequest req = buildRegisterRequest("ali@test.com", "Test123!", "Ali", "Veli", "5551234567");

        MvcResult result = mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("ali@test.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andReturn();

        Cookie accessCookie = result.getResponse().getCookie("access_token");
        Cookie refreshCookie = result.getResponse().getCookie("refresh_token");
        assertThat(accessCookie).isNotNull();
        assertThat(refreshCookie).isNotNull();
        assertThat(accessCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest req = buildRegisterRequest("dup@test.com", "Test123!", "Ali", "Veli", "5551234567");

        mockMvc.perform(post("/api/users/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("dup@test.com")));
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        RegisterRequest req = buildRegisterRequest("weak@test.com", "simple", "Ali", "Veli", "5551234567");

        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_missingEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setPassword("Test123!");
        req.setFirstName("Ali");
        req.setLastName("Veli");

        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- login ---------------------------------------------------------------

    @Test
    void login_success_returnsCookies() throws Exception {
        registerUser("login@test.com", "Test123!");

        LoginRequest req = new LoginRequest();
        req.setEmail("login@test.com");
        req.setPassword("Test123!");

        MvcResult result = mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("login@test.com"))
                .andReturn();

        assertThat(result.getResponse().getCookie("access_token")).isNotNull();
        assertThat(result.getResponse().getCookie("refresh_token")).isNotNull();
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        registerUser("cred@test.com", "Test123!");

        LoginRequest req = new LoginRequest();
        req.setEmail("cred@test.com");
        req.setPassword("WrongPass1!");

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_nonExistentEmail_returns401() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@test.com");
        req.setPassword("Test123!");

        mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // --- refresh -------------------------------------------------------------

    @Test
    void refresh_success_rotatesTokens() throws Exception {
        MvcResult loginResult = loginUser("refresh@test.com", "Test123!");
        Cookie oldRefresh = loginResult.getResponse().getCookie("refresh_token");
        Cookie oldAccess = loginResult.getResponse().getCookie("access_token");
        assertThat(oldRefresh).isNotNull();

        MvcResult refreshResult = mockMvc.perform(post("/api/users/auth/refresh")
                        .cookie(oldRefresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Cookie newAccess = refreshResult.getResponse().getCookie("access_token");
        Cookie newRefresh = refreshResult.getResponse().getCookie("refresh_token");
        assertThat(newAccess).isNotNull();
        assertThat(newRefresh).isNotNull();
        assertThat(newAccess.getValue()).isNotEqualTo(oldAccess.getValue());
        assertThat(newRefresh.getValue()).isNotEqualTo(oldRefresh.getValue());
    }

    @Test
    void refresh_reusedToken_returns401() throws Exception {
        MvcResult loginResult = loginUser("reuse@test.com", "Test123!");
        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/users/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void refresh_noCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/users/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    // --- logout --------------------------------------------------------------

    @Test
    void logout_success_clearsCookies() throws Exception {
        MvcResult loginResult = loginUser("logout@test.com", "Test123!");
        Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

        MvcResult logoutResult = mockMvc.perform(post("/api/users/auth/logout")
                        .cookie(accessCookie, refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Cookie clearedAccess = logoutResult.getResponse().getCookie("access_token");
        Cookie clearedRefresh = logoutResult.getResponse().getCookie("refresh_token");
        assertThat(clearedAccess).isNotNull();
        assertThat(clearedAccess.getMaxAge()).isZero();
        assertThat(clearedRefresh).isNotNull();
        assertThat(clearedRefresh.getMaxAge()).isZero();
    }

    @Test
    void logout_afterLogout_accessTokenBlacklisted() throws Exception {
        MvcResult loginResult = loginUser("blacklist@test.com", "Test123!");
        Cookie accessCookie = loginResult.getResponse().getCookie("access_token");
        Cookie refreshCookie = loginResult.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/users/auth/logout")
                        .cookie(accessCookie, refreshCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers -------------------------------------------------------------

    private void registerUser(String email, String password) throws Exception {
        RegisterRequest req = buildRegisterRequest(email, password, "Test", "User", "5550000000");
        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private MvcResult loginUser(String email, String password) throws Exception {
        registerUser(email, password);

        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);

        return mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private RegisterRequest buildRegisterRequest(String email, String password,
                                                  String firstName, String lastName,
                                                  String phone) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFirstName(firstName);
        req.setLastName(lastName);
        req.setPhoneNumber(phone);
        return req;
    }
}
