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

import java.util.concurrent.atomic.AtomicInteger;

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
class AddressControllerIT {

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

    private static final AtomicInteger PHONE_SEQ = new AtomicInteger(1000);

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void createAddress_firstAddress_autoDefault() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("addr@test.com", "Test123!");

        mockMvc.perform(post("/api/users/me/addresses")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddress("Ev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Ev"))
                .andExpect(jsonPath("$.data.default").value(true));
    }

    @Test
    void createAddress_secondAddress_notDefault() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("addr2@test.com", "Test123!");

        mockMvc.perform(post("/api/users/me/addresses")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddress("Ev"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/me/addresses")
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddress("İş"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.default").value(false));
    }

    @Test
    void getAddresses_returnsList() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("list@test.com", "Test123!");

        mockMvc.perform(post("/api/users/me/addresses")
                .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildAddress("Ev")))).andExpect(status().isOk());

        mockMvc.perform(post("/api/users/me/addresses")
                .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildAddress("İş")))).andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/addresses").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void updateAddress_changesFields() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("upd@test.com", "Test123!");

        MvcResult created = mockMvc.perform(post("/api/users/me/addresses")
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddress("Ev"))))
                .andExpect(status().isOk()).andReturn();

        Long addressId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        AddressRequest updated = buildAddress("Yazlık");
        updated.setCity("İzmir");

        mockMvc.perform(put("/api/users/me/addresses/" + addressId)
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Yazlık"))
                .andExpect(jsonPath("$.data.city").value("İzmir"));
    }

    @Test
    void deleteAddress_removesFromList() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("del@test.com", "Test123!");

        MvcResult created = mockMvc.perform(post("/api/users/me/addresses")
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddress("Ev"))))
                .andExpect(status().isOk()).andReturn();

        Long addressId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/api/users/me/addresses/" + addressId).cookie(cookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me/addresses").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void setDefaultAddress_changesDefault() throws Exception {
        Cookie cookie = loginAndGetAccessCookie("def@test.com", "Test123!");

        mockMvc.perform(post("/api/users/me/addresses")
                .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildAddress("Ev")))).andExpect(status().isOk());

        MvcResult second = mockMvc.perform(post("/api/users/me/addresses")
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddress("İş"))))
                .andExpect(status().isOk()).andReturn();

        Long secondId = objectMapper.readTree(second.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(put("/api/users/me/addresses/" + secondId + "/default").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.default").value(true));
    }

    @Test
    void createAddress_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/users/me/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddress("Ev"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateAddress_wrongOwner_returns404() throws Exception {
        Cookie owner = loginAndGetAccessCookie("owner@test.com", "Test123!");
        Cookie other = loginAndGetAccessCookie("other@test.com", "Test123!");

        MvcResult created = mockMvc.perform(post("/api/users/me/addresses")
                        .cookie(owner).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddress("Ev"))))
                .andExpect(status().isOk()).andReturn();

        Long addressId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(put("/api/users/me/addresses/" + addressId)
                        .cookie(other).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddress("Hırsız"))))
                .andExpect(status().isNotFound());
    }

    // -- helpers ---------------------------------------------------------------

    private Cookie loginAndGetAccessCookie(String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setFirstName("Test");
        reg.setLastName("User");
        reg.setPhoneNumber(String.format("555%07d", PHONE_SEQ.getAndIncrement()));

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

    private AddressRequest buildAddress(String title) {
        AddressRequest req = new AddressRequest();
        req.setTitle(title);
        req.setRecipientName("Test User");
        req.setRecipientPhone("5551234567");
        req.setCity("İstanbul");
        req.setDistrict("Kadıköy");
        req.setAddressLine("Test Sokak No:1");
        return req;
    }
}
