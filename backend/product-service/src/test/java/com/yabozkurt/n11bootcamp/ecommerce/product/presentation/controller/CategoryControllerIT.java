package com.yabozkurt.n11bootcamp.ecommerce.product.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.CategoryRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.service.ProductIndexService;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.publisher.ProductEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.minio.MinioService;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.CategoryRequest;
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
class CategoryControllerIT {

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
    @Autowired CategoryRepository categoryRepository;

    @BeforeEach
    void cleanUp() {
        categoryRepository.deleteAll();
    }

    @Test
    void createCategory_valid_returns200() throws Exception {
        CategoryRequest req = new CategoryRequest();
        req.setName("Elektronik");

        mockMvc.perform(post("/api/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Elektronik"))
                .andExpect(jsonPath("$.data.parentId").doesNotExist());
    }

    @Test
    void createCategory_duplicate_returns400() throws Exception {
        CategoryRequest req = new CategoryRequest();
        req.setName("Giyim");

        mockMvc.perform(post("/api/products/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());

        mockMvc.perform(post("/api/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_returnsCategories() throws Exception {
        CategoryRequest req = new CategoryRequest();
        req.setName("Spor");
        mockMvc.perform(post("/api/products/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))).andExpect(status().isOk());

        mockMvc.perform(get("/api/products/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void createSubCategory_withParent_returnsParentId() throws Exception {
        CategoryRequest parent = new CategoryRequest();
        parent.setName("Elektronik");
        String parentJson = mockMvc.perform(post("/api/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parent)))
                .andReturn().getResponse().getContentAsString();

        Long parentId = objectMapper.readTree(parentJson).path("data").path("id").asLong();

        CategoryRequest child = new CategoryRequest();
        child.setName("Telefon");
        child.setParentId(parentId);

        mockMvc.perform(post("/api/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(child)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parentId").value(parentId));
    }

    @Test
    void deleteCategory_existing_returns200() throws Exception {
        CategoryRequest req = new CategoryRequest();
        req.setName("Silinecek");
        String json = mockMvc.perform(post("/api/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(json).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/products/categories/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/products/categories/999999"))
                .andExpect(status().isNotFound());
    }
}
