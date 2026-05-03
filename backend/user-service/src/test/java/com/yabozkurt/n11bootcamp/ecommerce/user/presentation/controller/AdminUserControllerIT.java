package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.User;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.enums.Role;
import com.yabozkurt.n11bootcamp.ecommerce.user.domain.repository.UserRepository;
import com.yabozkurt.n11bootcamp.ecommerce.user.infrastructure.messaging.publisher.UserEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.LoginRequest;
import com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.request.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.yabozkurt.n11bootcamp.ecommerce.user.domain.model.enums.UserStatus;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
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
class AdminUserControllerIT {

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

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(2000);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Cookie adminCookie;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        adminCookie = createAdminAndLogin();
    }

    // -- getAllUsers ------------------------------------------------------------

    @Test
    void getAllUsers_asAdmin_returnsPagedList() throws Exception {
        registerUser("user1@test.com");
        registerUser("user2@test.com");

        mockMvc.perform(get("/api/users/admin").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getAllUsers_notAdmin_returns403() throws Exception {
        Cookie userCookie = registerAndLogin("regular@test.com", "Test123!");

        mockMvc.perform(get("/api/users/admin").cookie(userCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users/admin"))
                .andExpect(status().isUnauthorized());
    }

    // -- getUserById -----------------------------------------------------------

    @Test
    void getUserById_existingUser_returnsUser() throws Exception {
        Long userId = registerUserAndGetId("target@test.com");

        mockMvc.perform(get("/api/users/admin/{id}", userId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("target@test.com"));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/users/admin/99999").cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    // -- freezeUser ------------------------------------------------------------

    @Test
    void freezeUser_activeUser_freezesAccount() throws Exception {
        Long userId = registerUserAndGetId("freeze@test.com");

        mockMvc.perform(patch("/api/users/admin/{id}/freeze", userId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/users/admin/{id}", userId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FROZEN"));
    }

    @Test
    void freezeUser_notAdmin_returns403() throws Exception {
        Long userId = registerUserAndGetId("noadmin@test.com");
        Cookie userCookie = registerAndLogin("other@test.com", "Test123!");

        mockMvc.perform(patch("/api/users/admin/{id}/freeze", userId).cookie(userCookie))
                .andExpect(status().isForbidden());
    }

    // -- unfreezeUser ----------------------------------------------------------

    @Test
    void unfreezeUser_frozenUser_activatesAccount() throws Exception {
        Long userId = registerUserAndGetId("unfreeze@test.com");

        mockMvc.perform(patch("/api/users/admin/{id}/freeze", userId).cookie(adminCookie))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/users/admin/{id}/unfreeze", userId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/users/admin/{id}", userId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    // -- deleteUser ------------------------------------------------------------

    @Test
    void deleteUser_existingUser_softDeletes() throws Exception {
        Long userId = registerUserAndGetId("delete@test.com");

        mockMvc.perform(delete("/api/users/admin/{id}", userId).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(userRepository.findById(userId))
                .isPresent()
                .hasValueSatisfying(u -> assertThat(u.getStatus()).isEqualTo(UserStatus.DELETED));
    }

    @Test
    void deleteUser_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/users/admin/99999").cookie(adminCookie))
                .andExpect(status().isNotFound());
    }

    // -- helpers ---------------------------------------------------------------

    private Cookie createAdminAndLogin() throws Exception {
        User admin = new User(
                "admin@test.com",
                passwordEncoder.encode("Admin123!"),
                "Admin",
                "User",
                "5550000000",
                Role.ADMIN
        );
        userRepository.save(admin);

        LoginRequest login = new LoginRequest();
        login.setEmail("admin@test.com");
        login.setPassword("Admin123!");

        MvcResult result = mockMvc.perform(post("/api/users/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("access_token");
    }

    private void registerUser(String email) throws Exception {
        RegisterRequest req = buildRegister(email, "Test123!");
        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private Long registerUserAndGetId(String email) throws Exception {
        RegisterRequest req = buildRegister(email, "Test123!");
        mockMvc.perform(post("/api/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private Cookie registerAndLogin(String email, String password) throws Exception {
        RegisterRequest reg = buildRegister(email, password);
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

    private RegisterRequest buildRegister(String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setFirstName("Test");
        req.setLastName("User");
        req.setPhoneNumber(String.format("555%07d", PHONE_SEQ.getAndIncrement()));
        return req;
    }
}
