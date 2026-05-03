package com.yabozkurt.n11bootcamp.ecommerce.product.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.CategoryRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductVariantRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.service.ProductIndexService;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.publisher.ProductEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.minio.MinioService;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.CategoryRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.ProductRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.VariantRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;

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
class VariantControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("product_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
        registry.add("DATASOURCE_USERNAME", postgres::getUsername);
        registry.add("DATASOURCE_PASSWORD", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean MinioService minioService;
    @MockBean ProductEventPublisher eventPublisher;
    @MockBean ProductIndexService productIndexService;
    @Autowired ProductRepository productRepository;
    @Autowired ProductVariantRepository variantRepository;
    @Autowired CategoryRepository categoryRepository;

    private Long productId;

    @BeforeEach
    void setUp() throws Exception {
        variantRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        CategoryRequest catReq = new CategoryRequest();
        catReq.setName("Default");
        String catJson = mockMvc.perform(post("/api/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andReturn().getResponse().getContentAsString();
        Long categoryId = objectMapper.readTree(catJson).path("data").path("id").asLong();

        ProductRequest req = new ProductRequest();
        req.setName("Tişört");
        req.setDescription("Test description");
        req.setBrand("Brand");
        req.setCategoryId(categoryId);

        MvcResult result = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        productId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    void createVariant_valid_returns200() throws Exception {
        VariantRequest req = buildRequest("TSH-M-RED", new BigDecimal("299"), Map.of("beden", "M"));

        mockMvc.perform(post("/api/products/" + productId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("TSH-M-RED"))
                .andExpect(jsonPath("$.data.effectivePrice").value(299.0));
    }

    @Test
    void createVariant_duplicateSku_returns400() throws Exception {
        VariantRequest req = buildRequest("TSH-DUP", new BigDecimal("100"), Map.of("beden", "M"));

        mockMvc.perform(post("/api/products/" + productId + "/variants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());

        mockMvc.perform(post("/api/products/" + productId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_returnsVariants() throws Exception {
        mockMvc.perform(post("/api/products/" + productId + "/variants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        buildRequest("TSH-S", new BigDecimal("100"), Map.of("beden", "S"))))).andExpect(status().isOk());

        mockMvc.perform(post("/api/products/" + productId + "/variants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        buildRequest("TSH-L", new BigDecimal("150"), Map.of("beden", "L"))))).andExpect(status().isOk());

        mockMvc.perform(get("/api/products/" + productId + "/variants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void updateVariant_changesFields() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products/" + productId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest("TSH-M", new BigDecimal("299"), Map.of("beden", "M")))))
                .andReturn();

        Long variantId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        VariantRequest update = buildRequest("TSH-M-UPDATED", new BigDecimal("399"), Map.of("beden", "M"));

        mockMvc.perform(put("/api/products/" + productId + "/variants/" + variantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("TSH-M-UPDATED"))
                .andExpect(jsonPath("$.data.effectivePrice").value(399.0));
    }

    @Test
    void deleteVariant_returns200() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products/" + productId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildRequest("TSH-DEL", new BigDecimal("100"), Map.of("beden", "XL")))))
                .andReturn();

        Long variantId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/api/products/" + productId + "/variants/" + variantId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/" + productId + "/variants/" + variantId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/products/" + productId + "/variants/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createVariant_withAbsolutePrice_returnsCorrectEffectivePrice() throws Exception {
        VariantRequest req = buildRequest("TSH-XL", new BigDecimal("349"), Map.of("beden", "XL"));

        mockMvc.perform(post("/api/products/" + productId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.effectivePrice").value(349.0));
    }

    private VariantRequest buildRequest(String sku, BigDecimal price, Map<String, String> attributes) {
        VariantRequest req = new VariantRequest();
        req.setSku(sku);
        req.setPrice(price);
        req.setAttributes(attributes);
        return req;
    }
}
