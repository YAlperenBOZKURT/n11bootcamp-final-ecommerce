package com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.testcontainers.RedisContainer;
import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.repository.CartRepository;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.CouponClient;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.AddCartItemRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.UpdateCartItemQuantityRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

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
class CartControllerIT {

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("REDIS_HOST", redis::getHost);
        registry.add("REDIS_PORT", () -> redis.getMappedPort(6379));
    }

    @MockBean
    CouponClient couponClient;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CartRepository cartRepository;

    private static final String USER_ID = "1";

    @BeforeEach
    void cleanUp() {
        cartRepository.deleteAll();
    }

    // -- getCart ---------------------------------------------------------------

    @Test
    void getCart_emptyCart_returnsEmptyItems() throws Exception {
        mockMvc.perform(get("/api/cart").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void getCart_missingUserId_returns400() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isBadRequest());
    }

    // -- addItem ---------------------------------------------------------------

    @Test
    void addItem_validRequest_addsToCart() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddRequest(1L, "Ürün A", new BigDecimal("50.00"), 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(100.00));
    }

    @Test
    void addItem_sameProductTwice_incrementsQuantity() throws Exception {
        AddCartItemRequest req = buildAddRequest(1L, "Ürün A", new BigDecimal("50.00"), 1);
        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(100.00));
    }

    @Test
    void addItem_invalidRequest_returns400() throws Exception {
        AddCartItemRequest req = new AddCartItemRequest();

        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // -- updateItemQuantity ----------------------------------------------------

    @Test
    void updateItemQuantity_validId_updatesQuantity() throws Exception {
        String body = mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddRequest(2L, "Ürün B", new BigDecimal("30.00"), 1))))
                .andReturn().getResponse().getContentAsString();

        String itemId = objectMapper.readTree(body).path("data").path("items").get(0).path("itemId").asText();

        UpdateCartItemQuantityRequest updateReq = new UpdateCartItemQuantityRequest();
        setField(updateReq, "quantity", 3);

        mockMvc.perform(patch("/api/cart/items/{itemId}", itemId)
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(90.00));
    }

    // -- removeItem ------------------------------------------------------------

    @Test
    void removeItem_existingItem_removesFromCart() throws Exception {
        String body = mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddRequest(3L, "Ürün C", new BigDecimal("20.00"), 1))))
                .andReturn().getResponse().getContentAsString();

        String itemId = objectMapper.readTree(body).path("data").path("items").get(0).path("itemId").asText();

        mockMvc.perform(delete("/api/cart/items/{itemId}", itemId)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    // -- clearCart -------------------------------------------------------------

    @Test
    void clearCart_afterAddingItems_cartIsEmpty() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAddRequest(4L, "Ürün D", new BigDecimal("10.00"), 1))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/cart").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/cart").header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    // -- helpers ---------------------------------------------------------------

    private AddCartItemRequest buildAddRequest(Long productId, String name, BigDecimal price, int quantity) {
        AddCartItemRequest req = new AddCartItemRequest();
        req.setProductId(productId);
        req.setProductName(name);
        req.setUnitPrice(price);
        req.setQuantity(quantity);
        return req;
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not set field " + fieldName, e);
        }
    }
}
